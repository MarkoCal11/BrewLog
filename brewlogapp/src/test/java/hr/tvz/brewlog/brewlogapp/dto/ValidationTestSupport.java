package hr.tvz.brewlog.brewlogapp.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared setup for the DTO Bean Validation slice — builds a single {@link Validator} from the
 * default factory (no Spring context) and exposes small assertion helpers. Validating the request
 * records directly is the fast, context-free way to verify the {@code @NotBlank/@NotNull/@Size/...}
 * rules the controllers rely on.
 */
abstract class ValidationTestSupport {

    private static ValidatorFactory factory;
    protected static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeFactory() {
        factory.close();
    }

    /** Asserts the object is fully valid (no violations). */
    protected <T> void assertValid(T object) {
        assertThat(validator.validate(object)).isEmpty();
    }

    /** Asserts there is at least one violation on the given property path. */
    protected <T> void assertViolationOn(T object, String property) {
        Set<ConstraintViolation<T>> violations = validator.validate(object);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains(property);
    }
}
