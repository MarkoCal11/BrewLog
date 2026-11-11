package hr.tvz.brewlog.brewlogapp.config;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {
        if (password.isBlank()) {
            log.info("ADMIN_PASSWORD nije postavljen, admin se ne kreira");
            return;
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build());
        log.info("Kreiran admin korisnik '{}'", username);
    }
}