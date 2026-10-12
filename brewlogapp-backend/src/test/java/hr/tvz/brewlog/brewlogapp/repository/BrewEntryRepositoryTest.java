package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link BrewEntryRepository#findAverageRatingByCoffeeId} — the AVG
 * aggregate JPQL used to compute a bean's average rating (null-rating entries are ignored by AVG,
 * and a bean with no entries yields null).
 */
@RepositorySliceTest
class BrewEntryRepositoryTest {

    @Autowired private BrewEntryRepository repository;
    @Autowired private TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = em.persist(User.builder().username("alice").password("x").email("a@x.com")
                .role(Role.USER).active(true).build());
    }

    private CoffeeBean bean(String name) {
        return em.persist(CoffeeBean.builder()
                .name(name).origin("Ethiopia").roastLevel(RoastLevel.MEDIUM)
                .roasterName("Blue Bottle").user(user).build());
    }

    private void brew(CoffeeBean bean, Double rating) {
        em.persist(BrewEntry.builder()
                .method(Method.ESPRESSO).waterTempC(93.0).doseGrams(18.0)
                .brewedAt(LocalDateTime.now()).rating(rating).coffeeBean(bean).user(user)
                .build());
    }

    @Test
    void averagesRatingsForBean() {
        CoffeeBean bean = bean("Yirgacheffe");
        brew(bean, 4.0);
        brew(bean, 2.0);
        em.flush();
        em.clear();

        assertThat(repository.findAverageRatingByCoffeeId(bean.getId())).isEqualTo(3.0);
    }

    @Test
    void returnsNullWhenBeanHasNoEntries() {
        CoffeeBean bean = bean("Empty");
        em.flush();
        em.clear();

        assertThat(repository.findAverageRatingByCoffeeId(bean.getId())).isNull();
    }

    @Test
    void ignoresNullRatingsInAverage() {
        CoffeeBean bean = bean("Mixed");
        brew(bean, 4.0);
        brew(bean, null);
        em.flush();
        em.clear();

        assertThat(repository.findAverageRatingByCoffeeId(bean.getId())).isEqualTo(4.0);
    }

    @Test
    void scopesAverageToTheGivenBean() {
        CoffeeBean a = bean("A");
        CoffeeBean b = bean("B");
        brew(a, 5.0);
        brew(b, 1.0);
        em.flush();
        em.clear();

        assertThat(repository.findAverageRatingByCoffeeId(a.getId())).isEqualTo(5.0);
        assertThat(repository.findAverageRatingByCoffeeId(b.getId())).isEqualTo(1.0);
    }
}
