package hr.tvz.brewlog.brewlogapp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class LoginRequestTest extends ValidationTestSupport {

    @Test
    void validWhenUsernameAndPasswordPresent() {
        assertValid(new LoginRequest("alice", "secret", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidCases")
    void invalidFieldIsRejected(String desc, LoginRequest request, String property) {
        assertViolationOn(request, property);
    }

    static Stream<Arguments> invalidCases() {
        return Stream.of(
                Arguments.of("blank username", new LoginRequest("", "secret", false), "username"),
                Arguments.of("blank password", new LoginRequest("alice", "   ", false), "password")
        );
    }
}
