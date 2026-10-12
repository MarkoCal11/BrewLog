package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.CafeLocation;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link CafeLocationRepository}'s user-scoped finders — each user only
 * sees and counts their own cafe locations.
 */
@RepositorySliceTest
class CafeLocationRepositoryTest {

    @Autowired private CafeLocationRepository repository;
    @Autowired private TestEntityManager em;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        userA = em.persist(user("alice"));
        userB = em.persist(user("bob"));
    }

    private User user(String name) {
        return User.builder().username(name).password("x").email(name + "@x.com")
                .role(Role.USER).active(true).build();
    }

    private CafeLocation cafe(String name, User owner) {
        return em.persist(CafeLocation.builder().name(name).city("Zagreb").user(owner).build());
    }

    @Test
    void findAllByUserReturnsOnlyOwnersCafes() {
        CafeLocation mine = cafe("Eli's", userA);
        cafe("Cogito", userB);
        em.flush();
        em.clear();

        assertThat(repository.findAllByUser(userA)).extracting(CafeLocation::getId)
                .containsExactly(mine.getId());
    }

    @Test
    void findByIdAndUserHonoursOwnership() {
        CafeLocation mine = cafe("Eli's", userA);
        CafeLocation theirs = cafe("Cogito", userB);
        em.flush();
        em.clear();

        assertThat(repository.findByIdAndUser(mine.getId(), userA)).isPresent();
        assertThat(repository.findByIdAndUser(theirs.getId(), userA)).isEmpty();
    }

    @Test
    void countByUserIdCountsOnlyThatUsersCafes() {
        cafe("Eli's", userA);
        cafe("Quahwa", userA);
        cafe("Cogito", userB);
        em.flush();
        em.clear();

        assertThat(repository.countByUser_Id(userA.getId())).isEqualTo(2);
    }
}
