package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: verifies the HTTP contract the frontend consumes — status codes plus the
 * uniform {@code { "message": "..." }} body produced by
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}.
 *
 * <p>Filters are disabled ({@code addFilters = false}); {@link WithMockUser} populates the
 * {@code @AuthenticationPrincipal}. Authorization-rule coverage belongs to the security suite.
 */
@ControllerSliceTest(BrewEntryController.class)
@WithMockUser(username = "barista")
class BrewEntryControllerTest {

    private static final String USERNAME = "barista";
    private static final String BASE = "/api/brewEntries";

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

    private static final BrewEntryResponse SENTINEL = new BrewEntryResponse(
            1L, Method.ESPRESSO, 18.0, 93.0, 28.0, 18.0, 36.0, 36.0, "nice", 4.0, null, BEAN);

    private static final String VALID_CREATE_JSON = """
            {"method":"ESPRESSO","doseGrams":18.0,"brewedAt":"2026-06-14T10:00:00","coffeeBeanId":5}""";

    private static final String MISSING_METHOD_JSON = """
            {"doseGrams":18.0,"brewedAt":"2026-06-14T10:00:00","coffeeBeanId":5}""";

    private static final String VALID_UPDATE_JSON = """
            {"method":"ESPRESSO","waterTempC":93.0,"doseGrams":18.0,"brewedAt":"2026-06-14T10:00:00","coffeeBeanId":5}""";

    // ---- create ----

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(brewEntryService.createBrewEntry(any(), eq(USERNAME))).thenReturn(SENTINEL);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/brewEntries/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.method").value("ESPRESSO"));
    }

    @Test
    void createWithUnknownBeanReturns400WithMessage() throws Exception {
        when(brewEntryService.createBrewEntry(any(), eq(USERNAME)))
                .thenThrow(new IllegalArgumentException("Coffee bean not found: 5"));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coffee bean not found: 5"));
    }

    @Test
    void createMissingMethodReturns400FromValidation() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(MISSING_METHOD_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- delete ----

    @Test
    void deleteReturns204WhenRemoved() throws Exception {
        when(brewEntryService.removeBrewEntryById(7L, USERNAME)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(brewEntryService.removeBrewEntryById(7L, USERNAME)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- update ----

    @Test
    void updateReturns200WhenOwned() throws Exception {
        when(brewEntryService.updateBrewEntry(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(brewEntryService.updateBrewEntry(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isNotFound());
    }
}
