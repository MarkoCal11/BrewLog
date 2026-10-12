package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Method;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.stream.Stream;

class BrewEntryCreateRequestTest extends ValidationTestSupport {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Test
    void validWhenRequiredFieldsPresentAndRatingInRange() {
        assertValid(new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                4.0, "bright", NOW, 1L));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, BrewEntryCreateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("null method", new BrewEntryCreateRequest(null, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                        4.0, "bright", NOW, 1L), "method"),
                Arguments.of("null dose", new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, null, 250.0, 240.0,
                        4.0, "bright", NOW, 1L), "doseGrams"),
                Arguments.of("null brewedAt", new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                        4.0, "bright", null, 1L), "brewedAt"),
                Arguments.of("null coffeeBeanId", new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                        4.0, "bright", NOW, null), "coffeeBeanId"),
                Arguments.of("rating below 1", new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                        0.0, "bright", NOW, 1L), "rating"),
                Arguments.of("rating above 5", new BrewEntryCreateRequest(Method.V60, 18.0, 94.0, 150.0, 15.0, 250.0, 240.0,
                        6.0, "bright", NOW, 1L), "rating")
        );
    }
}
