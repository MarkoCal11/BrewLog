package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AuthUserResponse;
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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    private ResponseCookie buildRefreshCookie(String refreshToken, long ttlSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(ttlSeconds)
                .sameSite(cookieSameSite)
                .build();
    }

    private ResponseCookie buildClearCookie() {
        return ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .sameSite(cookieSameSite)
                .build();
    }

    /** Mobile clients (Capacitor) send {@code X-Client: mobile} since they can't use the cookie. */
    private static boolean isMobile(String clientHeader) {
        return "mobile".equalsIgnoreCase(clientHeader);
    }

    private static boolean hasToken(RefreshRequest body) {
        return body != null && body.refreshToken() != null && !body.refreshToken().isBlank();
    }

    /**
     * Mobile: refresh token in the body, no cookie. Web: refresh token in the HttpOnly cookie,
     * omitted from the body.
     */
    private ResponseEntity<JwtResponse> tokenResponse(String accessToken, AuthUserResponse authUser,
                                                      String refreshToken, long ttlSeconds, boolean mobile) {
        if (mobile) {
            return ResponseEntity.ok(new JwtResponse(accessToken, authUser, refreshToken));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, ttlSeconds).toString())
                .body(new JwtResponse(accessToken, authUser, null));
    }

    /** 401 that clears the cookie for web; mobile has no cookie to clear. */
    private ResponseEntity<JwtResponse> unauthorized(boolean mobile) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.UNAUTHORIZED);
        if (!mobile) {
            builder.header(HttpHeaders.SET_COOKIE, buildClearCookie().toString());
        }
        return builder.build();
    }

    private ResponseEntity<JwtResponse> issueTokenPair(User user, UserDetails userDetails,
                                                       boolean mobile, boolean rememberMe) {
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshTokenService.RefreshResult issued = refreshTokenService.issue(user, rememberMe);
        AuthUserResponse authUser = new AuthUserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getDisplayName(), user.getFirstName(), user.getLastName());
        return tokenResponse(accessToken, authUser, issued.rawToken(), issued.ttl().toSeconds(), mobile);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request,
                                             @RequestHeader(value = "X-Client", required = false) String client) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        User user = userRepository.findByUsername(request.username())
                .orElseThrow();
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.username());
        return issueTokenPair(user, userDetails, isMobile(client), request.rememberMe());
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest body) {
        // Mobile carries the refresh token in the body; web reads it from the cookie.
        boolean mobile = hasToken(body);
        String refreshToken = mobile ? body.refreshToken() : cookieToken;
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            RefreshTokenService.RefreshResult result = refreshTokenService.rotate(refreshToken);
            User user = result.user();

            if (!user.isActive()) {
                refreshTokenService.revokeByRawToken(result.rawToken());
                return unauthorized(mobile);
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());

            String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
            AuthUserResponse authUser = new AuthUserResponse(user.getId(), user.getUsername(), user.getRole().name(), user.getDisplayName(), user.getFirstName(), user.getLastName());
            return tokenResponse(newAccessToken, authUser, result.rawToken(), result.ttl().toSeconds(), mobile);
        } catch (RefreshTokenException _) {
            return unauthorized(mobile);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest body) {
        boolean mobile = hasToken(body);
        String refreshToken = mobile ? body.refreshToken() : cookieToken;
        if (refreshToken != null) {
            refreshTokenService.revokeByRawToken(refreshToken);
        }
        if (mobile) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, buildClearCookie().toString())
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<Object> register(@Valid @RequestBody RegisterRequest request,
                                           @RequestHeader(value = "X-Client", required = false) String client) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new RegisterResponse("Username already exists"));
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.USER)
                .build();
        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        ResponseEntity<JwtResponse> response = issueTokenPair(user, userDetails, isMobile(client), false);
        return ResponseEntity.status(response.getStatusCode())
                .headers(response.getHeaders())
                .body(response.getBody());
    }
}
