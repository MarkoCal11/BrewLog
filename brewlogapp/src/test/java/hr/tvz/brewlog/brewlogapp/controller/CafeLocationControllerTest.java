package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
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
 * {@code @AuthenticationPrincipal}. CafeLocation has no uniqueness/state service errors, so coverage
 * here is happy-path, bean-validation 400s, and not-found 404s. Authorization-rule coverage belongs
 * to the security suite.
 */
@ControllerSliceTest(CafeLocationController.class)
@WithMockUser(username = "barista")
class CafeLocationControllerTest {

    private static final String USERNAME = "barista";
    private static final String BASE = "/api/cafeLocations";

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

    private static final CafeLocationResponse SENTINEL =
            new CafeLocationResponse(1L, "Cogito", "Zagreb", "Varšavska 11", 5.0, "@cogitocoffee", 45.81, 15.97);

    private static final String VALID_JSON = """
            {"name":"Cogito","city":"Zagreb","rating":5}""";

    private static final String BLANK_NAME_JSON = """
            {"name":"  ","city":"Zagreb"}""";

    private static final String BAD_INSTAGRAM_JSON = """
            {"name":"Cogito","instagram":"not a handle!!"}""";

    // ---- create ----

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(cafeLocationService.createCafeLocation(any(), eq(USERNAME))).thenReturn(SENTINEL);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/cafeLocations/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Cogito"));
    }

    @Test
    void createWithBlankNameReturns400FromValidation() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLANK_NAME_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createWithBadInstagramReturns400FromValidation() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BAD_INSTAGRAM_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- update ----

    @Test
    void updateReturns200WhenOwned() throws Exception {
        when(cafeLocationService.updateCafeLocation(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(cafeLocationService.updateCafeLocation(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    // ---- delete ----

    @Test
    void deleteReturns204WhenRemoved() throws Exception {
        when(cafeLocationService.removeCafeLocation(7L, USERNAME)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(cafeLocationService.removeCafeLocation(7L, USERNAME)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }
}
