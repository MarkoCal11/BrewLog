package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminBrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.BrewEntryService;
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
 * Web-layer slice test for the admin (global) brew-entry endpoints. Verifies status codes plus the
 * uniform {@code { "message": "..." }} body from
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}. Authorization-rule coverage
 * (ADMIN-only access) belongs to the security suite.
 */
@ControllerSliceTest(AdminBrewEntryController.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminBrewEntryControllerTest {

    private static final String BASE = "/api/admin/brewEntries";

    @Autowired private MockMvc mockMvc;

    @TestConfiguration
    static class PrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean private BrewEntryService brewEntryService;

    // Required only so the auto-included JwtAuthenticationFilter (a Filter bean) can be constructed;
    // it never runs because addFilters = false.
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailServiceImpl userDetailService;

    private static final CoffeeBeanResponse BEAN = new CoffeeBeanResponse(
            5L, "Yirgacheffe", "Ethiopia", RoastLevel.MEDIUM, null, "Blue Bottle",
            0.0, 0, null, null, null, null, false, null, false, null, List.of());

    private static final AdminBrewEntryResponse SENTINEL = new AdminBrewEntryResponse(
            1L, Method.ESPRESSO, 18.0, 93.0, 28.0, 18.0, 36.0, 36.0, "nice", 4.0, null, BEAN, 9L, "owner");

    private static final String VALID_UPDATE_JSON = """
            {"method":"ESPRESSO","waterTempC":93.0,"doseGrams":18.0,"brewedAt":"2026-06-14T10:00:00","coffeeBeanId":5}""";

    // ---- get by id ----

    @Test
    void getByIdReturns200WhenFound() throws Exception {
        when(brewEntryService.findBrewEntryByIdGlobal(7L)).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(get(BASE + "/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.ownerUsername").value("owner"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(brewEntryService.findBrewEntryByIdGlobal(7L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- delete ----

    @Test
    void deleteReturns204WhenRemoved() throws Exception {
        when(brewEntryService.removeBrewEntryByIdGlobal(7L)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(brewEntryService.removeBrewEntryByIdGlobal(7L)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- update ----

    @Test
    void updateReturns200WhenFound() throws Exception {
        when(brewEntryService.updateBrewEntryGlobal(eq(7L), any())).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(brewEntryService.updateBrewEntryGlobal(eq(7L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isNotFound());
    }
}
