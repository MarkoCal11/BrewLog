package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminCafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.CafeLocationService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test for the admin (global) cafe-location endpoints. Verifies status codes plus
 * the uniform {@code { "message": "..." }} body from
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}. Authorization-rule coverage
 * (ADMIN-only access) belongs to the security suite.
 */
@ControllerSliceTest(AdminCafeLocationController.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminCafeLocationControllerTest {

    private static final String BASE = "/api/admin/cafeLocations";

    @Autowired private MockMvc mockMvc;

    @TestConfiguration
    static class PrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean private CafeLocationService cafeLocationService;

    // Required only so the auto-included JwtAuthenticationFilter (a Filter bean) can be constructed;
    // it never runs because addFilters = false.
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailServiceImpl userDetailService;

    private static final AdminCafeLocationResponse SENTINEL = new AdminCafeLocationResponse(
            1L, "Cogito", "Zagreb", "Varšavska 11", 5.0, "@cogitocoffee", 45.81, 15.97, 9L, "owner");

    private static final String VALID_JSON = """
            {"name":"Cogito","city":"Zagreb","rating":5}""";

    // ---- get by id ----

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        when(cafeLocationService.findCafeLocationByIdGlobal(7L)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(get(BASE + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerUsername").value("owner"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(cafeLocationService.findCafeLocationByIdGlobal(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- update ----

    @Test
    void updateReturns200WhenFound() throws Exception {
        when(cafeLocationService.updateCafeLocationGlobal(eq(7L), any())).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(cafeLocationService.updateCafeLocationGlobal(eq(7L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    // ---- delete ----

    @Test
    void deleteReturns204WhenRemoved() throws Exception {
        when(cafeLocationService.removeCafeLocationGlobal(7L)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(cafeLocationService.removeCafeLocationGlobal(7L)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }
}
