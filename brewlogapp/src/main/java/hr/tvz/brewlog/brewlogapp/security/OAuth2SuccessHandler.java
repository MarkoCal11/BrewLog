package hr.tvz.brewlog.brewlogapp.security;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import hr.tvz.brewlog.brewlogapp.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Value("${jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @Value("${jwt.cookie-same-site:Lax}")
    private String cookieSameSite;

    private static final String REFRESH_COOKIE = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final UserDetailsService userDetailsService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String googleId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");

        User user = resolveUser(googleId, email, name, givenName, familyName);

        if (!user.isActive()) {
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/oauth2/error?reason=inactive");
            return;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        RefreshTokenService.RefreshResult issued = refreshTokenService.issue(user, false);

        // The OAuth2 session was only needed for the redirect dance (state parameter).
        // Invalidate it now so it can't bleed into subsequent JWT-authenticated API calls.
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Set the refresh token as an httpOnly cookie — mirrors the regular login flow
        // so the frontend refresh mechanism works identically after OAuth2 login.
        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE, issued.rawToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(issued.ttl().toSeconds())
                .sameSite(cookieSameSite)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        // Only the access token goes in the URL — refresh token is in the cookie.
        String redirect = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/callback")
                .queryParam("token", accessToken)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, redirect);
    }

    private User resolveUser(String googleId, String email, String name, String givenName, String familyName) {
        // 1. Known Google user — fast path
        return userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        // 2. Existing email/password account — link it
                        .map(existing -> {
                            existing.setGoogleId(googleId);
                            if (existing.getDisplayName() == null) existing.setDisplayName(name);
                            if (existing.getFirstName() == null) existing.setFirstName(givenName);
                            if (existing.getLastName() == null) existing.setLastName(familyName);
                            return userRepository.save(existing);
                        })
                        // 3. Brand new user
                        .orElseGet(() -> userRepository.save(User.builder()
                                .username(email)
                                .password("OAUTH2_NO_PASSWORD")
                                .email(email)
                                .googleId(googleId)
                                .displayName(name)
                                .firstName(givenName)
                                .lastName(familyName)
                                .role(Role.USER)
                                .build())));
    }
}
