package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.enums.Category;
import hr.tvz.brewlog.brewlogapp.exception.DuplicateResourceException;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.FlavorNoteService;
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
 * Web-layer slice test: verifies the HTTP contract the admin frontend consumes —
 * status codes plus the uniform {@code { "message": "..." }} body produced by
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}.
 *
 * <p>Filters are disabled ({@code addFilters = false}); {@link WithMockUser} still populates the
 * {@code @AuthenticationPrincipal}. Authorization-rule coverage (role/JWT enforcement — i.e. that
 * only ADMINs reach these endpoints) belongs to the dedicated security test suite, not here.
 */
@ControllerSliceTest(AdminFlavorNoteController.class)
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminFlavorNoteControllerTest {

    private static final String BASE = "/api/admin/flavorNotes";

    @Autowired private MockMvc mockMvc;

    /**
     * The slice doesn't load the app's {@code @EnableWebSecurity} config, so the
     * {@code @AuthenticationPrincipal} resolver isn't registered. Registered here to keep the
     * template uniform with the other controller slices.
     */
    @TestConfiguration
    static class PrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean private FlavorNoteService flavorNoteService;

    // Required only so the auto-included JwtAuthenticationFilter (a Filter bean) can be constructed;
    // it never runs because addFilters = false.
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailServiceImpl userDetailService;

    private static final FlavorNoteResponse SENTINEL =
            new FlavorNoteResponse(1L, "Blueberry", Category.FRUITY, true);

    private static final String VALID_JSON = """
            {"name":"Blueberry","category":"FRUITY"}""";

    private static final String BLANK_NAME_JSON = """
            {"name":"  ","category":"FRUITY"}""";

    // ---- create ----

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(flavorNoteService.createFlavorNoteGlobal(any())).thenReturn(SENTINEL);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/admin/flavorNotes/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.global").value(true));
    }

    @Test
    void createDuplicateReturns409WithMessage() throws Exception {
        when(flavorNoteService.createFlavorNoteGlobal(any()))
                .thenThrow(new DuplicateResourceException("This flavor already exists."));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This flavor already exists."));
    }

    @Test
    void createWithBlankNameReturns400FromValidation() throws Exception {
        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BLANK_NAME_JSON))
                .andExpect(status().isBadRequest());
    }

    // ---- delete ----

    @Test
    void deleteReturns204WhenRemoved() throws Exception {
        when(flavorNoteService.removeFlavorNoteGlobal(7L)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(flavorNoteService.removeFlavorNoteGlobal(7L)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }

    // ---- update ----

    @Test
    void updateReturns200WhenFound() throws Exception {
        when(flavorNoteService.updateFlavorNoteGlobal(eq(7L), any())).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(flavorNoteService.updateFlavorNoteGlobal(eq(7L), any())).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRenameCollisionReturns409WithMessage() throws Exception {
        when(flavorNoteService.updateFlavorNoteGlobal(eq(7L), any()))
                .thenThrow(new DuplicateResourceException("This flavor already exists."));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This flavor already exists."));
    }
}
