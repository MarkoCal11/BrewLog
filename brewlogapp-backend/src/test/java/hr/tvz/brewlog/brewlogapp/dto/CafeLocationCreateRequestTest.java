package hr.tvz.brewlog.brewlogapp.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class CafeLocationCreateRequestTest extends ValidationTestSupport {

    @ParameterizedTest(name = "{0}")
    @MethodSource("validCases")
    void validCasesPass(String desc, CafeLocationCreateRequest request) {
        assertValid(request);
    }

    static Stream<Arguments> validCases() {
        return Stream.of(
                Arguments.of("all fields in range",
                        new CafeLocationCreateRequest("Eli's", "Zagreb", "Ilica 1", 5.0, "@elis", 45.8, 16.0)),
                Arguments.of("instagram handle",
                        new CafeLocationCreateRequest("Eli's", null, null, null, "@cafe", null, null)),
                Arguments.of("instagram url",
                        new CafeLocationCreateRequest("Eli's", null, null, null, "https://instagram.com/cafe", null, null)),
                Arguments.of("empty instagram",
                        new CafeLocationCreateRequest("Eli's", null, null, null, "", null, null))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, CafeLocationCreateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank name",
                        new CafeLocationCreateRequest(" ", "Zagreb", "Ilica 1", 5.0, "@elis", 45.8, 16.0), "name"),
                Arguments.of("rating below 1",
                        new CafeLocationCreateRequest("Eli's", null, null, 0.0, null, null, null), "rating"),
                Arguments.of("rating above 5",
                        new CafeLocationCreateRequest("Eli's", null, null, 6.0, null, null, null), "rating"),
                Arguments.of("malformed instagram",
                        new CafeLocationCreateRequest("Eli's", null, null, null, "not a handle!", null, null), "instagram"),
                Arguments.of("latitude out of range",
                        new CafeLocationCreateRequest("Eli's", null, null, null, null, 91.0, null), "latitude"),
                Arguments.of("longitude out of range",
                        new CafeLocationCreateRequest("Eli's", null, null, null, null, null, -181.0), "longitude")
        );
    }
}
