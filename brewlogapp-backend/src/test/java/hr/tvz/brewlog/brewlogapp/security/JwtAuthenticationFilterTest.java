package hr.tvz.brewlog.brewlogapp.security;

import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link JwtAuthenticationFilter}: which conditions populate the
 * {@code SecurityContext} and which leave it empty. Notably the {@code userDetails.isEnabled()} gate
 * (deactivated accounts must NOT authenticate) — our custom filter enforces this itself rather than
 * relying on Spring's enabled-check.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String TOKEN = "the.access.token";

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private UserDetailServiceImpl userDetailService;

    @InjectMocks private JwtAuthenticationFilter filter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    @Mock private FilterChain chain;

    private static final UserDetails ENABLED =
            User.withUsername("bob").password("x").authorities("ROLE_USER").build();
    private static final UserDetails DISABLED =
            User.withUsername("bob").password("x").disabled(true).authorities("ROLE_USER").build();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void bearer() {
        request.addHeader("Authorization", "Bearer " + TOKEN);
    }

    private static Object currentPrincipal() {
        return SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Test
    void noHeaderLeavesContextEmptyAndProceeds() throws Exception {
        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void nonBearerHeaderLeavesContextEmpty() throws Exception {
        request.addHeader("Authorization", "Basic abc");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void nonAccessTokenTypeDoesNotAuthenticate() throws Exception {
        bearer();
        when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("bob");
        when(jwtTokenProvider.extractTokenType(TOKEN)).thenReturn("REFRESH");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void validAccessTokenForEnabledUserAuthenticates() throws Exception {
        bearer();
        when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("bob");
        when(jwtTokenProvider.extractTokenType(TOKEN)).thenReturn("ACCESS");
        when(userDetailService.loadUserByUsername("bob")).thenReturn(ENABLED);
        when(jwtTokenProvider.isTokenValid(TOKEN, ENABLED)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(currentPrincipal()).isEqualTo(ENABLED);
        verify(chain).doFilter(request, response);
    }

    @Test
    void disabledUserIsNotAuthenticated() throws Exception {
        bearer();
        when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("bob");
        when(jwtTokenProvider.extractTokenType(TOKEN)).thenReturn("ACCESS");
        when(userDetailService.loadUserByUsername("bob")).thenReturn(DISABLED);
        when(jwtTokenProvider.isTokenValid(TOKEN, DISABLED)).thenReturn(true);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidTokenDoesNotAuthenticate() throws Exception {
        bearer();
        when(jwtTokenProvider.extractUsername(TOKEN)).thenReturn("bob");
        when(jwtTokenProvider.extractTokenType(TOKEN)).thenReturn("ACCESS");
        when(userDetailService.loadUserByUsername("bob")).thenReturn(ENABLED);
        when(jwtTokenProvider.isTokenValid(TOKEN, ENABLED)).thenReturn(false);

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void jwtExceptionClearsContextAndStillProceeds() throws Exception {
        bearer();
        when(jwtTokenProvider.extractUsername(TOKEN)).thenThrow(new MalformedJwtException("bad"));

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
