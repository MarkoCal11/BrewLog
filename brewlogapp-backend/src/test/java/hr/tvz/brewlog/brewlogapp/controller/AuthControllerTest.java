package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.JwtResponse;
import hr.tvz.brewlog.brewlogapp.dto.LoginRequest;
import hr.tvz.brewlog.brewlogapp.dto.RefreshRequest;
import hr.tvz.brewlog.brewlogapp.dto.RegisterRequest;
import hr.tvz.brewlog.brewlogapp.dto.RegisterResponse;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.RefreshTokenException;
import hr.tvz.brewlog.brewlogapp.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthController}: the auth-flow decisions that live in the controller itself —
 * status codes, the refresh-cookie lifecycle (issue / rotate / clear), the inactive-account guard on
 * refresh, and duplicate-username handling on register.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserDetailsService userDetailsService;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthController controller;

    private static final UserDetails DETAILS =
            org.springframework.security.core.userdetails.User
                    .withUsername("bob").password("x").authorities("ROLE_USER").build();

    @BeforeEach
    void setCookieProps() {
        ReflectionTestUtils.setField(controller, "cookieSecure", false);
        ReflectionTestUtils.setField(controller, "cookieSameSite", "Lax");
    }

    private User user(boolean active) {
        return User.builder().id(1L).username("bob").password("x").role(Role.USER).active(active).build();
    }

    private static String setCookie(ResponseEntity<?> response) {
        return response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
    }

    // ---- login ----

    @Test
    void loginIssuesAccessTokenAndRefreshCookie() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user(true)));
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(DETAILS);
        when(jwtTokenProvider.generateAccessToken(DETAILS)).thenReturn("access-jwt");
        when(refreshTokenService.issue(any(User.class), anyBoolean()))
                .thenReturn(new RefreshTokenService.RefreshResult(user(true), "raw-refresh", RefreshTokenService.REFRESH_TTL));

        ResponseEntity<JwtResponse> response = controller.login(new LoginRequest("bob", "pw", false), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo("access-jwt");
        assertThat(response.getBody().user().username()).isEqualTo("bob");
        assertThat(response.getBody().refreshToken()).as("web keeps refresh token out of the body").isNull();
        assertThat(setCookie(response)).contains("refreshToken=raw-refresh").contains("HttpOnly");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void loginMobileReturnsRefreshTokenInBodyAndNoCookie() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user(true)));
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(DETAILS);
        when(jwtTokenProvider.generateAccessToken(DETAILS)).thenReturn("access-jwt");
        when(refreshTokenService.issue(any(User.class), anyBoolean()))
                .thenReturn(new RefreshTokenService.RefreshResult(user(true), "raw-refresh", RefreshTokenService.REFRESH_TTL));

        ResponseEntity<JwtResponse> response = controller.login(new LoginRequest("bob", "pw", false), "mobile");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isEqualTo("access-jwt");
        assertThat(response.getBody().refreshToken()).isEqualTo("raw-refresh");
        assertThat(setCookie(response)).as("mobile gets no refresh cookie").isNull();
    }

    // ---- refresh ----

    @Test
    void refreshWithoutCookieReturns401() {
        ResponseEntity<JwtResponse> response = controller.refresh(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void refreshRotatesCookieAndReturnsNewAccessToken() {
        when(refreshTokenService.rotate("old-raw"))
                .thenReturn(new RefreshTokenService.RefreshResult(user(true), "new-raw", RefreshTokenService.REFRESH_TTL));
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(DETAILS);
        when(jwtTokenProvider.generateAccessToken(DETAILS)).thenReturn("access-2");

        ResponseEntity<JwtResponse> response = controller.refresh("old-raw", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isEqualTo("access-2");
        assertThat(response.getBody().refreshToken()).as("web rotates via cookie, not body").isNull();
        assertThat(setCookie(response)).contains("refreshToken=new-raw");
    }

    @Test
    void refreshMobileRotatesViaBodyAndReturnsNewTokenInBody() {
        when(refreshTokenService.rotate("old-raw"))
                .thenReturn(new RefreshTokenService.RefreshResult(user(true), "new-raw", RefreshTokenService.REFRESH_TTL));
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(DETAILS);
        when(jwtTokenProvider.generateAccessToken(DETAILS)).thenReturn("access-2");

        ResponseEntity<JwtResponse> response = controller.refresh(null, new RefreshRequest("old-raw"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().accessToken()).isEqualTo("access-2");
        assertThat(response.getBody().refreshToken()).isEqualTo("new-raw");
        assertThat(setCookie(response)).as("mobile gets no refresh cookie").isNull();
    }

    @Test
    void refreshForInactiveUserRevokesAndReturns401() {
        when(refreshTokenService.rotate("old-raw"))
                .thenReturn(new RefreshTokenService.RefreshResult(user(false), "new-raw", RefreshTokenService.REFRESH_TTL));

        ResponseEntity<JwtResponse> response = controller.refresh("old-raw", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNull();
        verify(refreshTokenService).revokeByRawToken("new-raw");
        assertThat(setCookie(response)).contains("refreshToken=").contains("Max-Age=0");
    }

    @Test
    void refreshWithInvalidTokenReturns401AndClearsCookie() {
        when(refreshTokenService.rotate("bad-raw")).thenThrow(new RefreshTokenException("invalid"));

        ResponseEntity<JwtResponse> response = controller.refresh("bad-raw", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(setCookie(response)).contains("Max-Age=0");
    }

    // ---- logout ----

    @Test
    void logoutWithCookieRevokesAndClears() {
        ResponseEntity<Void> response = controller.logout("raw-refresh", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(refreshTokenService).revokeByRawToken("raw-refresh");
        assertThat(setCookie(response)).contains("Max-Age=0");
    }

    @Test
    void logoutWithoutCookieStillClearsAndDoesNotRevoke() {
        ResponseEntity<Void> response = controller.logout(null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(refreshTokenService, never()).revokeByRawToken(any());
        assertThat(setCookie(response)).contains("Max-Age=0");
    }

    @Test
    void logoutMobileRevokesBodyTokenWithoutClearingCookie() {
        ResponseEntity<Void> response = controller.logout(null, new RefreshRequest("raw-refresh"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(refreshTokenService).revokeByRawToken("raw-refresh");
        assertThat(setCookie(response)).as("mobile has no cookie to clear").isNull();
    }

    // ---- register ----

    @Test
    void registerDuplicateUsernameReturns409() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user(true)));

        ResponseEntity<Object> response =
                controller.register(new RegisterRequest("bob", "pw", "bob@example.com", "Bob", "Smith"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(RegisterResponse.class);
        assertThat(((RegisterResponse) response.getBody()).message()).isEqualTo("Username already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerNewUserPersistsAndIssuesTokens() {
        when(userRepository.findByUsername("bob")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pw")).thenReturn("hashed");
        when(userDetailsService.loadUserByUsername("bob")).thenReturn(DETAILS);
        when(jwtTokenProvider.generateAccessToken(DETAILS)).thenReturn("access-jwt");
        when(refreshTokenService.issue(any(User.class), anyBoolean()))
                .thenReturn(new RefreshTokenService.RefreshResult(user(true), "raw-refresh", RefreshTokenService.REFRESH_TTL));

        ResponseEntity<Object> response =
                controller.register(new RegisterRequest("bob", "pw", "bob@example.com", "Bob", "Smith"), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(JwtResponse.class);
        assertThat(((JwtResponse) response.getBody()).accessToken()).isEqualTo("access-jwt");
        assertThat(setCookie(response)).contains("refreshToken=raw-refresh");
        verify(userRepository).save(any(User.class));
    }
}
