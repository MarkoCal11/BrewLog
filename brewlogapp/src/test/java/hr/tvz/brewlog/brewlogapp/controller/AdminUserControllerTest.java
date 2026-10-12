package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminUserStats;
import hr.tvz.brewlog.brewlogapp.dto.UserResponse;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: verifies the HTTP contract the admin frontend consumes —
 * status codes plus the uniform {@code { "message": "..." }} body produced by
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}, including the last-admin guard
 * (→ 409) and missing-user (→ 404) paths.
 *
 * <p>Filters are disabled ({@code addFilters = false}); {@link WithMockUser} supplies the
 * {@code @AuthenticationPrincipal} actor the mutating endpoints read. Authorization-rule coverage
 * (role/JWT enforcement — i.e. that only ADMINs reach these endpoints) belongs to the dedicated
 * security test suite, not here.
 */
@ControllerSliceTest(AdminUserController.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminUserControllerTest {

    private static final String ACTOR = "admin";
    private static final String BASE = "/api/admin/users";

    @Autowired private MockMvc mockMvc;

    /**
     * The slice doesn't load the app's {@code @EnableWebSecurity} config, so the
     * {@code @AuthenticationPrincipal} resolver isn't registered. This controller's mutating
     * endpoints read the actor principal, so the resolver is required here — it injects the mock
     * user from {@link WithMockUser} (kept in the {@code SecurityContextHolder}).
     */
    @TestConfiguration
    static class PrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean private AdminUserService adminUserService;

    // Required only so the auto-included JwtAuthenticationFilter (a Filter bean) can be constructed;
    // it never runs because addFilters = false.
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailServiceImpl userDetailService;

    private static final UserResponse SENTINEL =
            new UserResponse(1L, "bob", "bob@example.com", "USER", true, null);

    private static final String LAST_ADMIN_MESSAGE = "Cannot deactivate or demote the last active admin.";

    private static final String ROLE_JSON = """
            {"role":"USER"}""";

    private static final String MISSING_ROLE_JSON = "{}";

    // ---- get by id ----

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        when(adminUserService.findUserById(7L)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(get(BASE + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("bob"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(adminUserService.findUserById(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- stats ----

    @Test
    void getStatsReturns200WhenFound() throws Exception {
        when(adminUserService.findUserStats(7L)).thenReturn(Optional.of(new AdminUserStats(3, 12, 1)));

        mockMvc.perform(get(BASE + "/7/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beanCount").value(3))
                .andExpect(jsonPath("$.brewCount").value(12));
    }

    @Test
    void getStatsReturns404WhenMissing() throws Exception {
        when(adminUserService.findUserStats(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/7/stats")).andExpect(status().isNotFound());
    }

    // ---- update role ----

    @Test
    void updateRoleReturns200WhenFound() throws Exception {
        when(adminUserService.updateUserRole(7L, Role.USER, ACTOR)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROLE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateRoleReturns404WhenMissing() throws Exception {
        when(adminUserService.updateUserRole(7L, Role.USER, ACTOR)).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROLE_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRoleMissingRoleReturns400FromValidation() throws Exception {
        mockMvc.perform(put(BASE + "/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSING_ROLE_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRoleDemotingLastAdminReturns409WithMessage() throws Exception {
        when(adminUserService.updateUserRole(7L, Role.USER, ACTOR))
                .thenThrow(new IllegalStateException(LAST_ADMIN_MESSAGE));

        mockMvc.perform(put(BASE + "/7/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROLE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(LAST_ADMIN_MESSAGE));
    }

    // ---- deactivate ----

    @Test
    void deactivateReturns200WhenFound() throws Exception {
        when(adminUserService.deactivateUser(7L, ACTOR)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(post(BASE + "/7/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deactivateReturns404WhenMissing() throws Exception {
        when(adminUserService.deactivateUser(7L, ACTOR)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/7/deactivate")).andExpect(status().isNotFound());
    }

    @Test
    void deactivateLastAdminReturns409WithMessage() throws Exception {
        when(adminUserService.deactivateUser(7L, ACTOR))
                .thenThrow(new IllegalStateException(LAST_ADMIN_MESSAGE));

        mockMvc.perform(post(BASE + "/7/deactivate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(LAST_ADMIN_MESSAGE));
    }

    // ---- reactivate ----

    @Test
    void reactivateReturns200WhenFound() throws Exception {
        when(adminUserService.reactivateUser(7L, ACTOR)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(post(BASE + "/7/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void reactivateReturns404WhenMissing() throws Exception {
        when(adminUserService.reactivateUser(7L, ACTOR)).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE + "/7/reactivate")).andExpect(status().isNotFound());
    }
}
