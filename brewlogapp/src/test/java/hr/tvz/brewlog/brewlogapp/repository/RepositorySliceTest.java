package hr.tvz.brewlog.brewlogapp.repository;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shared setup for repository slice tests. Bundles {@link DataJpaTest} with the H2 properties this
 * project's repositories need, and pulls in {@link JacksonAutoConfiguration} so the JSON
 * {@code AttributeConverter} beans (which require a Jackson {@code ObjectMapper}) can be constructed
 * — {@code @DataJpaTest} alone does not load the Jackson auto-configuration, which otherwise fails
 * EntityManagerFactory creation for the whole slice.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@DataJpaTest
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.properties.hibernate.auto_quote_keyword=true"
})
public @interface RepositorySliceTest {
}
