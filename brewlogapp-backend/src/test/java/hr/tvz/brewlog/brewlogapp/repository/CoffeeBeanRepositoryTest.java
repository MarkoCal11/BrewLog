package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link CoffeeBeanRepository}'s derived finders — user-scoping (a user
 * only ever sees their own beans), the {@code countByUser_Id} stat, and the case-insensitive
 * name+roaster lookups (both the user-scoped overload used for per-user dedup and the global
 * overload), which the duplicate-detection logic relies on.
 */
@RepositorySliceTest
class CoffeeBeanRepositoryTest {

    @Autowired private CoffeeBeanRepository repository;
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

    private CoffeeBean bean(String name, String origin, String roaster, User owner) {
        return em.persist(CoffeeBean.builder()
                .name(name).origin(origin).roasterName(roaster)
                .roastLevel(RoastLevel.MEDIUM).user(owner).build());
    }

    @Test
    void findAllByUserReturnsOnlyOwnersBeans() {
        CoffeeBean mine = bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        bean("Geisha", "Panama", "Onyx", userB);
        em.flush();
        em.clear();

        assertThat(repository.findAllByUser(userA)).extracting(CoffeeBean::getId)
                .containsExactly(mine.getId());
    }

    @Test
    void findByIdAndUserHonoursOwnership() {
        CoffeeBean mine = bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        CoffeeBean theirs = bean("Geisha", "Panama", "Onyx", userB);
        em.flush();
        em.clear();

        assertThat(repository.findByIdAndUser(mine.getId(), userA)).isPresent();
        assertThat(repository.findByIdAndUser(theirs.getId(), userA)).isEmpty();
    }

    @Test
    void countByUserIdCountsOnlyThatUsersBeans() {
        bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        bean("Sidamo", "Ethiopia", "Blue Bottle", userA);
        bean("Geisha", "Panama", "Onyx", userB);
        em.flush();
        em.clear();

        assertThat(repository.countByUser_Id(userA.getId())).isEqualTo(2);
    }

    @Test
    void findByNameAndUserMatchesExactNameForOwner() {
        bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        em.flush();
        em.clear();

        assertThat(repository.findByNameAndUser("Yirgacheffe", userA)).isPresent();
        assertThat(repository.findByNameAndUser("Yirgacheffe", userB)).isEmpty();
    }

    @Test
    void findByOriginAndUserFiltersByOriginAndOwner() {
        bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        bean("Sidamo", "Ethiopia", "Onyx", userA);
        bean("Geisha", "Panama", "Onyx", userA);
        bean("Sidamo", "Ethiopia", "Onyx", userB);
        em.flush();
        em.clear();

        assertThat(repository.findByOriginAndUser("Ethiopia", userA)).hasSize(2);
    }

    @Test
    void findByNameAndRoasterIgnoreCaseAndUserIsCaseInsensitiveAndScoped() {
        bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        em.flush();
        em.clear();

        assertThat(repository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(
                "yirgacheffe", "blue bottle", userA)).hasSize(1);
        assertThat(repository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(
                "yirgacheffe", "blue bottle", userB)).isEmpty();
    }

    @Test
    void findByNameAndRoasterIgnoreCaseGlobalOverloadIgnoresOwner() {
        bean("Yirgacheffe", "Ethiopia", "Blue Bottle", userA);
        bean("YIRGACHEFFE", "Ethiopia", "BLUE BOTTLE", userB);
        em.flush();
        em.clear();

        List<CoffeeBean> result = repository.findByNameIgnoreCaseAndRoasterNameIgnoreCase(
                "yirgacheffe", "blue bottle");

        assertThat(result).hasSize(2);
    }
}
