package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.model.RefreshToken;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.RefreshTokenRepository;
import hr.tvz.brewlog.brewlogapp.security.RefreshTokenException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks private RefreshTokenService service;

    private final User user = User.builder().id(1L).username("barista").build();

    @Test
    void issuePersistsUnusedTokenAndReturnsRawValue() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshResult issued = service.issue(user, false);

        assertThat(issued.rawToken()).isNotBlank();
        assertThat(issued.ttl()).isEqualTo(RefreshTokenService.REFRESH_TTL);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.isRememberMe()).isFalse();
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getTokenHash()).isNotEqualTo(issued.rawToken());
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
    }

    @Test
    void issueWithRememberMeUsesLongerTtl() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshResult issued = service.issue(user, true);

        assertThat(issued.ttl()).isEqualTo(RefreshTokenService.REMEMBER_ME_TTL);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();
        assertThat(saved.isRememberMe()).isTrue();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plus(Duration.ofDays(29)));
    }

    @Test
    void rotateThrowsWhenTokenUnknown() {
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("whatever"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    void rotateRevokesAllSessionsWhenReuseDetected() {
        RefreshToken used = RefreshToken.builder()
                .user(user).used(true)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("reuse");

        verify(refreshTokenRepository).deleteByUser(user);
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void rotateRejectsAndDeletesExpiredToken() {
        RefreshToken expired = RefreshToken.builder()
                .user(user).used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate("raw"))
                .isInstanceOf(RefreshTokenException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expired);
    }

    @Test
    void rotateMarksCurrentUsedAndIssuesSuccessor() {
        RefreshToken current = RefreshToken.builder()
                .user(user).used(false).rememberMe(false)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshResult result = service.rotate("raw");

        assertThat(current.isUsed()).isTrue();
        assertThat(result.user()).isSameAs(user);
        assertThat(result.rawToken()).isNotBlank();
        assertThat(result.ttl()).isEqualTo(RefreshTokenService.REFRESH_TTL);
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotatePropagatessRememberMeFlag() {
        RefreshToken current = RefreshToken.builder()
                .user(user).used(false).rememberMe(true)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
        when(refreshTokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.of(current));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshResult result = service.rotate("raw");

        assertThat(result.ttl()).isEqualTo(RefreshTokenService.REMEMBER_ME_TTL);
    }

    @Test
    void revokeDeletesMatchingTokenWhenPresent() {
        RefreshToken token = RefreshToken.builder().user(user).build();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

        service.revokeByRawToken("raw");

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void revokeIsNoOpWhenTokenAbsent() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        service.revokeByRawToken("raw");

        verify(refreshTokenRepository, never()).delete(any(RefreshToken.class));
    }
}
