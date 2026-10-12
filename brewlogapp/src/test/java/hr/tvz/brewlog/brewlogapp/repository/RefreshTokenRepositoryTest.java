package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.RefreshToken;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link RefreshTokenRepository#deleteAllExpired} — the {@code @Modifying}
 * bulk-delete JPQL that purges tokens whose {@code expiresAt} is before the cutoff, returning the
 * number deleted and leaving still-valid tokens intact.
 */
@RepositorySliceTest
class RefreshTokenRepositoryTest {

    @Autowired private RefreshTokenRepository repository;
    @Autowired private TestEntityManager em;

    private User user;

    @BeforeEach
    void setUp() {
        user = em.persist(User.builder().username("alice").password("x").email("a@x.com")
                .role(Role.USER).active(true).build());
    }

    private void token(String hash, LocalDateTime expiresAt) {
        em.persist(RefreshToken.builder()
                .tokenHash(hash).user(user).expiresAt(expiresAt)
                .used(false).createdAt(LocalDateTime.now())
                .build());
    }

    @Test
    void deletesOnlyExpiredTokensAndReturnsCount() {
        LocalDateTime now = LocalDateTime.now();
        token("expired", now.minusDays(1));
        token("valid", now.plusDays(1));
        em.flush();

        int deleted = repository.deleteAllExpired(now);
        em.clear();

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findByTokenHash("expired")).isEmpty();
        assertThat(repository.findByTokenHash("valid")).isPresent();
    }

    @Test
    void deletesNothingWhenAllTokensAreValid() {
        LocalDateTime now = LocalDateTime.now();
        token("valid-1", now.plusHours(1));
        token("valid-2", now.plusDays(2));
        em.flush();

        int deleted = repository.deleteAllExpired(now);
        em.clear();

        assertThat(deleted).isZero();
        assertThat(repository.count()).isEqualTo(2);
    }
}
