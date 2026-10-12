package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class CoffeeBeanCreateRequestTest extends ValidationTestSupport {

    @Test
    void validWhenRequiredFieldsPresent() {
        assertValid(new CoffeeBeanCreateRequest("Yirgacheffe", "Ethiopia", RoastLevel.MEDIUM, null,
                "Blue Bottle", null, null, null, null, null));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, CoffeeBeanCreateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank name", new CoffeeBeanCreateRequest("", "Ethiopia", RoastLevel.MEDIUM, null,
                        "Blue Bottle", null, null, null, null, null), "name"),
                Arguments.of("blank origin", new CoffeeBeanCreateRequest("Yirgacheffe", " ", RoastLevel.MEDIUM, null,
                        "Blue Bottle", null, null, null, null, null), "origin"),
                Arguments.of("null roast level", new CoffeeBeanCreateRequest("Yirgacheffe", "Ethiopia", null, null,
                        "Blue Bottle", null, null, null, null, null), "roastLevel"),
                Arguments.of("blank roaster name", new CoffeeBeanCreateRequest("Yirgacheffe", "Ethiopia", RoastLevel.MEDIUM, null,
                        "", null, null, null, null, null), "roasterName")
        );
    }
}
