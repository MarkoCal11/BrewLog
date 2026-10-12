package hr.tvz.brewlog.brewlogapp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class RegisterRequestTest extends ValidationTestSupport {

    @Test
    void validWhenAllFieldsPresentAndEmailWellFormed() {
        assertValid(new RegisterRequest("alice", "secret", "alice@x.com", "Alice", "Smith"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, RegisterRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank username", new RegisterRequest("  ", "secret", "alice@x.com", "Alice", "Smith"), "username"),
                Arguments.of("blank password", new RegisterRequest("alice", "", "alice@x.com", "Alice", "Smith"), "password"),
                Arguments.of("blank email", new RegisterRequest("alice", "secret", "", "Alice", "Smith"), "email"),
                Arguments.of("malformed email", new RegisterRequest("alice", "secret", "not-an-email", "Alice", "Smith"), "email"),
                Arguments.of("blank firstName", new RegisterRequest("alice", "secret", "alice@x.com", "", "Smith"), "firstName"),
                Arguments.of("blank lastName", new RegisterRequest("alice", "secret", "alice@x.com", "Alice", ""), "lastName")
        );
    }
}
