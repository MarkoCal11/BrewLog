package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.model.CafeLocation;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CafeLocationRepository extends JpaRepository<CafeLocation, Long> {

    long countByUser_Id(Long userId);

    List<CafeLocation> findAllByUser(User user);

    Optional<CafeLocation> findByIdAndUser(Long id, User user);
}
