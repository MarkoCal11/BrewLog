package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);

    long countByRoleAndActiveTrue(Role role);
}
