package hr.tvz.brewlog.brewlogapp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class CafeLocationUpdateRequestTest extends ValidationTestSupport {

    @Test
    void validWhenNamePresentAndOptionalFieldsInRange() {
        assertValid(new CafeLocationUpdateRequest("Eli's", "Zagreb", "Ilica 1", 4.0, "@elis", 45.8, 16.0));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, CafeLocationUpdateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank name",
                        new CafeLocationUpdateRequest("", "Zagreb", "Ilica 1", 4.0, "@elis", 45.8, 16.0), "name"),
                Arguments.of("rating out of range",
                        new CafeLocationUpdateRequest("Eli's", null, null, 6.0, null, null, null), "rating"),
                Arguments.of("malformed instagram",
                        new CafeLocationUpdateRequest("Eli's", null, null, null, "not a handle!", null, null), "instagram"),
                Arguments.of("latitude out of range",
                        new CafeLocationUpdateRequest("Eli's", null, null, null, null, -91.0, null), "latitude"),
                Arguments.of("longitude out of range",
                        new CafeLocationUpdateRequest("Eli's", null, null, null, null, null, 181.0), "longitude")
        );
    }
}
