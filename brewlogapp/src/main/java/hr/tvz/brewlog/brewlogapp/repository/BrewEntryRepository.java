package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface BrewEntryRepository extends JpaRepository<BrewEntry, Long> {

    @Query("SELECT AVG(b.rating) FROM BrewEntry b WHERE b.coffeeBean.id =:beanId")
    Double findAverageRatingByCoffeeId(@Param("beanId") Long coffeeId);

    Integer countByCoffeeBean_Id(Long coffeeBeanId);

    boolean existsByCoffeeBean_Id(Long coffeeBeanId);

    long countByUser_Id(Long userId);

    List<BrewEntry> findAllByUser(User user);

    Optional<BrewEntry> findByIdAndUser(Long id, User user);

    List<BrewEntry> findByUserAndBrewedAtGreaterThanEqualAndBrewedAtLessThan(
            User user, LocalDateTime fromInclusive, LocalDateTime toExclusive);

    List<BrewEntry> findByBrewedAtGreaterThanEqualAndBrewedAtLessThan(
            LocalDateTime fromInclusive, LocalDateTime toExclusive);
}
