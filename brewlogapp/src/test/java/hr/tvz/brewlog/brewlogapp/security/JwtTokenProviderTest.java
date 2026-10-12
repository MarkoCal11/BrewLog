package hr.tvz.brewlog.brewlogapp.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link JwtTokenProvider}: token round-trip and the validation predicate used by the
 * auth filter. The {@code base64-secret} normally injected via {@code @Value} is set by reflection.
 */
class JwtTokenProviderTest {

    // 32 bytes → valid HS256 key, base64-encoded the way the provider expects.
    private static final String SECRET =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private JwtTokenProvider provider;

    private static final UserDetails BOB =
            User.withUsername("bob").password("x").authorities("ROLE_USER").build();

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "base64Secret", SECRET);
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
    }

    @Test
    void generatedTokenRoundTripsUsernameAndType() {
        String token = provider.generateAccessToken(BOB);

        assertThat(provider.extractUsername(token)).isEqualTo("bob");
        assertThat(provider.extractTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    void isTokenValidTrueForMatchingUser() {
        String token = provider.generateAccessToken(BOB);

        assertThat(provider.isTokenValid(token, BOB)).isTrue();
    }

    @Test
    void isTokenValidFalseForDifferentUser() {
        String token = provider.generateAccessToken(BOB);
        UserDetails alice = User.withUsername("alice").password("x").authorities("ROLE_USER").build();

        assertThat(provider.isTokenValid(token, alice)).isFalse();
    }

    @Test
    void isTokenValidFalseForMalformedToken() {
        assertThat(provider.isTokenValid("not-a-jwt", BOB)).isFalse();
    }

    @Test
    void isTokenValidFalseForTamperedSignature() {
        String token = provider.generateAccessToken(BOB);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(provider.isTokenValid(tampered, BOB)).isFalse();
    }

    @Test
    void isTokenValidFalseForExpiredToken() {
        long past = System.currentTimeMillis() - 120_000L;
        String expired = Jwts.builder()
                .subject("bob")
                .claim("token_type", "ACCESS")
                .issuedAt(new Date(past))
                .expiration(new Date(past + 1_000L))
                .signWith(signingKey())
                .compact();

        assertThat(provider.isTokenValid(expired, BOB)).isFalse();
    }
}
