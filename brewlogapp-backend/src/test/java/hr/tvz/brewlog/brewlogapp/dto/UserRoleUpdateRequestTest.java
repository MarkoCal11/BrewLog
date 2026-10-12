package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.Role;
import org.junit.jupiter.api.Test;

class UserRoleUpdateRequestTest extends ValidationTestSupport {

    @Test
    void validWhenRolePresent() {
        assertValid(new UserRoleUpdateRequest(Role.ADMIN));
    }

    @Test
    void nullRoleIsRejected() {
        assertViolationOn(new UserRoleUpdateRequest(null), "role");
    }
}
