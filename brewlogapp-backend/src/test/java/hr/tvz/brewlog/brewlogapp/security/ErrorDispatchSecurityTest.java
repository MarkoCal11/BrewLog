package hr.tvz.brewlog.brewlogapp.security;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression guard for the error-dispatch bug: every handled error (validation 400, access-denied
 * 403, any 500) was being clobbered into 401 because the servlet ERROR dispatch to /error ran back
 * through the security chain with no authentication (the OncePerRequestFilter JWT filter skips error
 * dispatches). Fix: SecurityConfig permits DispatcherType.ERROR/ASYNC.
 *
 * <p>Must run on a real server (RANDOM_PORT) — MockMvc does not replay the container error dispatch
 * through the filter chain, so the slice tests cannot catch this. Uses the JDK HttpClient because
 * Spring Boot 4 no longer ships TestRestTemplate.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:errdispatch;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.quartz.auto-startup=false",
                "app.upload-dir=target/brewlog-regtest-uploads"
        })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ErrorDispatchSecurityTest {

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();
    private String userToken;

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @BeforeAll
    void registerUserAndCaptureToken() throws Exception {
        String body = "{\"username\":\"errdispatch_user\",\"password\":\"testpass1\",\"email\":\"errd@brew.com\",\"firstName\":\"Test\",\"lastName\":\"User\"}";
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url("/api/auth/register")))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build());
        assertThat(resp.statusCode()).isBetween(200, 299);

        Matcher m = Pattern.compile("\"accessToken\"\\s*:\\s*\"([^\"]+)\"").matcher(resp.body());
        assertThat(m.find()).as("register response carries an accessToken").isTrue();
        userToken = m.group(1);
        assertThat(userToken).isNotBlank();
    }

    @Test
    void validationFailureReturns400NotClobberedTo401() throws Exception {
        // blank name fails bean validation; the /error re-dispatch must keep the 400
        String invalidBean = "{\"name\":\"\",\"origin\":\"X\",\"roastLevel\":\"LIGHT\",\"roasterName\":\"Y\"}";
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url("/api/coffeeBeans")))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + userToken)
                .POST(HttpRequest.BodyPublishers.ofString(invalidBean))
                .build());

        assertThat(resp.statusCode()).isEqualTo(400);
    }

    @Test
    void authenticatedUserOnAdminRouteReturns403NotClobberedTo401() throws Exception {
        // authenticated USER hitting an ADMIN route -> 403 (the documented frontend 401-vs-403 contract)
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url("/api/admin/users")))
                .header("Authorization", "Bearer " + userToken)
                .GET()
                .build());

        assertThat(resp.statusCode()).isEqualTo(403);
    }

    @Test
    void missingTokenStillReturns401() throws Exception {
        // control: genuine unauthenticated access must REMAIN 401 after the dispatcher-type permit
        HttpResponse<String> resp = send(HttpRequest.newBuilder(URI.create(url("/api/coffeeBeans")))
                .GET()
                .build());

        assertThat(resp.statusCode()).isEqualTo(401);
    }
}
