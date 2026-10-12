package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Category;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FlavorNoteRepository extends JpaRepository<FlavorNote, Long> {

    Optional<FlavorNote> findByIdAndUser(Long id, User user);

    @Query("select f from FlavorNote f where f.category = :category and (f.user = :user or f.user is null)")
    List<FlavorNote> findByCategoryVisibleToUser(Category category, User user);

    Optional<FlavorNote> findByNameIgnoreCaseAndUser(String name, User user);

    @Query("select f from FlavorNote f where lower(f.name) = lower(:name) and (f.user = :user or f.user is null)")
    Optional<FlavorNote> findByNameIgnoreCaseVisibleToUser(String name, User user);

    @Query("select f from FlavorNote f join f.coffeeBean b " +
            "where b.id = :coffeeBeanId and (f.user = :user or f.user is null)")
    List<FlavorNote> findByCoffeeBeanIdVisibleToUser(Long coffeeBeanId, User user);

    @Query("select f from FlavorNote f where f.user = :user or f.user is null")
    List<FlavorNote> findAllVisibleToUser(User user);

    @Query("select f from FlavorNote f where f.id = :id and (f.user = :user or f.user is null)")
    Optional<FlavorNote> findByIdVisibleToUser(Long id, User user);

    List<FlavorNote> findAllByUserIsNull();

    Optional<FlavorNote> findByIdAndUserIsNull(Long id);

    Optional<FlavorNote> findByNameIgnoreCaseAndUserIsNull(String name);
}
