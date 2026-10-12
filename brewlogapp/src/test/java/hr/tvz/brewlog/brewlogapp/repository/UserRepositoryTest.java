package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link UserRepository}. Covers {@code findByUsername} (used by
 * authentication) and {@code countByRoleAndActiveTrue}, which backs the last-admin guard — the
 * count must exclude deactivated admins, otherwise the guard could be defeated.
 */
@RepositorySliceTest
class UserRepositoryTest {

    @Autowired private UserRepository repository;
    @Autowired private TestEntityManager em;

    private User user(String name, Role role, boolean active) {
        return em.persist(User.builder().username(name).password("x").email(name + "@x.com")
                .role(role).active(active).build());
    }

    @Test
    void findByUsernameReturnsMatch() {
        user("alice", Role.USER, true);
        em.flush();
        em.clear();

        assertThat(repository.findByUsername("alice")).isPresent()
                .get().extracting(User::getUsername).isEqualTo("alice");
    }

    @Test
    void findByUsernameIsEmptyWhenUnknown() {
        assertThat(repository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void countByRoleAndActiveTrueCountsOnlyActiveAdmins() {
        user("admin1", Role.ADMIN, true);
        user("admin2", Role.ADMIN, true);
        user("ghostAdmin", Role.ADMIN, false);   // deactivated — must not count
        user("user1", Role.USER, true);          // wrong role — must not count
        em.flush();
        em.clear();

        assertThat(repository.countByRoleAndActiveTrue(Role.ADMIN)).isEqualTo(2);
    }

    @Test
    void countByRoleAndActiveTrueIsZeroWhenAllAdminsDeactivated() {
        user("ghostAdmin", Role.ADMIN, false);
        em.flush();
        em.clear();

        assertThat(repository.countByRoleAndActiveTrue(Role.ADMIN)).isZero();
    }
}
