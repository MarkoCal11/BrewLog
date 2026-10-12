package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class FlavorNoteUpdateRequestTest extends ValidationTestSupport {

    @Test
    void validWhenNameAndCategoryPresent() {
        assertValid(new FlavorNoteUpdateRequest("Blueberry", Category.FRUITY));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, FlavorNoteUpdateRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank name", new FlavorNoteUpdateRequest("", Category.FRUITY), "name"),
                Arguments.of("null category", new FlavorNoteUpdateRequest("Blueberry", null), "category")
        );
    }
}
