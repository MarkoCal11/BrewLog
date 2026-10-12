package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for the read-only admin audit-log endpoint. Verifies the paged 200 contract.
 * Authorization-rule coverage (ADMIN-only access) belongs to the security suite.
 */
@ControllerSliceTest(AdminAuditLogController.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminAuditLogControllerTest {

    private static final String BASE = "/api/admin/audit-logs";

    @Autowired private MockMvc mockMvc;

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

    @Test
    void getAuditLogsReturns200WithPagedBody() throws Exception {
        AuditLogResponse entry =
                new AuditLogResponse(1L, "admin", 9L, "bob", null, "USER", "ADMIN", null);
        Page<AuditLogResponse> page = new PageImpl<>(List.of(entry));
        when(adminUserService.findAuditLogs(eq(9L), any())).thenReturn(page);

        mockMvc.perform(get(BASE).param("targetUserId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
