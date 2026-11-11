package hr.tvz.brewlog.brewlogapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:contextdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"jwt.base64-secret=dGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQtdGVzdC1zZWNyZXQ=",
		"spring.security.oauth2.client.registration.google.client-id=test",
		"spring.security.oauth2.client.registration.google.client-secret=test"
})
class BrewlogappApplicationTests {

	@Test
	void contextLoads() {
	}

}
