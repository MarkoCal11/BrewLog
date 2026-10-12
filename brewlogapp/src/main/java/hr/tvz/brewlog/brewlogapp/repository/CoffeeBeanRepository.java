package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoffeeBeanRepository extends JpaRepository<CoffeeBean, Long> {

    long countByUser_Id(Long userId);

    List<CoffeeBean> findAllByUser(User user);

    Optional<CoffeeBean> findByIdAndUser(Long id, User user);

    Optional<CoffeeBean> findByNameAndUser(String name, User user);

    List<CoffeeBean> findByOriginAndUser(String origin, User user);

    List<CoffeeBean> findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(String name, String roasterName, User user);

    List<CoffeeBean> findByNameIgnoreCaseAndRoasterNameIgnoreCase(String name, String roasterName);
}