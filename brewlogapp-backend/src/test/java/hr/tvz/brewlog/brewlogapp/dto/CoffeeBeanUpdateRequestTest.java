package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class CoffeeBeanUpdateRequestTest extends ValidationTestSupport {

    @Test
    void validWhenRequiredFieldsPresent() {
        assertValid(new CoffeeBeanUpdateRequest("Yirgacheffe", "Ethiopia", RoastLevel.MEDIUM, null,
                "Blue Bottle", null, null, null, null, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, CoffeeBeanUpdateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank name", new CoffeeBeanUpdateRequest("", "Ethiopia", RoastLevel.MEDIUM, null,
                        "Blue Bottle", null, null, null, null, null), "name"),
                Arguments.of("null roast level", new CoffeeBeanUpdateRequest("Yirgacheffe", "Ethiopia", null, null,
                        "Blue Bottle", null, null, null, null, null), "roastLevel")
        );
    }
}
