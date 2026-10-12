package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Category;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link FlavorNoteRepository}'s custom visibility JPQL — the hybrid model
 * where a row with a null owner is global (visible to everyone) and a row with an owner is private to
 * that user (see the flavor-notes-model decision). Schema comes from the entities (create-drop on an
 * embedded H2); {@code schema.sql} is disabled so the test depends only on the mappings.
 */
@RepositorySliceTest
class FlavorNoteRepositoryTest {

    @Autowired private FlavorNoteRepository repository;
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

    private FlavorNote note(String name, Category category, User owner) {
        return em.persist(FlavorNote.builder().name(name).category(category).user(owner).build());
    }

    @Test
    void findAllVisibleToUserReturnsGlobalPlusOwnButNotOthers() {
        FlavorNote global = note("Citrus", Category.FRUITY, null);
        FlavorNote mine = note("Blueberry", Category.FRUITY, userA);
        note("Walnut", Category.NUTTY, userB);
        em.flush();
        em.clear();

        List<FlavorNote> visible = repository.findAllVisibleToUser(userA);

        assertThat(visible).extracting(FlavorNote::getId)
                .containsExactlyInAnyOrder(global.getId(), mine.getId());
    }

    @Test
    void findByIdVisibleToUserHonoursOwnership() {
        FlavorNote global = note("Citrus", Category.FRUITY, null);
        FlavorNote mine = note("Blueberry", Category.FRUITY, userA);
        FlavorNote theirs = note("Walnut", Category.NUTTY, userB);
        em.flush();
        em.clear();

        assertThat(repository.findByIdVisibleToUser(global.getId(), userA)).isPresent();
        assertThat(repository.findByIdVisibleToUser(mine.getId(), userA)).isPresent();
        assertThat(repository.findByIdVisibleToUser(theirs.getId(), userA)).isEmpty();
    }

    @Test
    void findByCategoryVisibleToUserFiltersByCategoryAndVisibility() {
        FlavorNote global = note("Citrus", Category.FRUITY, null);
        FlavorNote mineFruity = note("Blueberry", Category.FRUITY, userA);
        note("Almond", Category.NUTTY, userA);          // wrong category
        note("Mango", Category.FRUITY, userB);          // not visible to A
        em.flush();
        em.clear();

        List<FlavorNote> result = repository.findByCategoryVisibleToUser(Category.FRUITY, userA);

        assertThat(result).extracting(FlavorNote::getId)
                .containsExactlyInAnyOrder(global.getId(), mineFruity.getId());
    }

    @Test
    void findByNameIgnoreCaseVisibleToUserMatchesCaseInsensitivelyAndRespectsVisibility() {
        note("BlueBerry", Category.FRUITY, userA);
        em.flush();
        em.clear();

        Optional<FlavorNote> found = repository.findByNameIgnoreCaseVisibleToUser("blueberry", userA);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("BlueBerry");
    }

    @Test
    void findByNameIgnoreCaseVisibleToUserDoesNotSeeOtherUsersNote() {
        note("Mango", Category.FRUITY, userB);
        em.flush();
        em.clear();

        assertThat(repository.findByNameIgnoreCaseVisibleToUser("mango", userA)).isEmpty();
    }

    @Test
    void findByCoffeeBeanIdVisibleToUserJoinsAndFiltersVisibility() {
        FlavorNote global = note("Citrus", Category.FRUITY, null);
        FlavorNote mine = note("Blueberry", Category.FRUITY, userA);
        FlavorNote theirs = note("Mango", Category.FRUITY, userB);

        CoffeeBean bean = CoffeeBean.builder()
                .name("Yirgacheffe").origin("Ethiopia").roastLevel(RoastLevel.MEDIUM)
                .roasterName("Blue Bottle").user(userA)
                .flavorNotes(Set.of(global, mine, theirs))
                .build();
        Long beanId = em.persist(bean).getId();
        em.flush();
        em.clear();

        List<FlavorNote> result = repository.findByCoffeeBeanIdVisibleToUser(beanId, userA);

        assertThat(result).extracting(FlavorNote::getId)
                .containsExactlyInAnyOrder(global.getId(), mine.getId());
    }
}
