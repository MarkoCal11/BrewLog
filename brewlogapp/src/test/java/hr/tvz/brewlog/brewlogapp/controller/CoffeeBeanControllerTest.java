package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.exception.DuplicateResourceException;
import hr.tvz.brewlog.brewlogapp.security.JwtTokenProvider;
import hr.tvz.brewlog.brewlogapp.security.UserDetailServiceImpl;
import hr.tvz.brewlog.brewlogapp.service.CoffeeBeanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test: verifies the HTTP contract the frontend actually consumes —
 * status codes plus the uniform {@code { "message": "..." }} body produced by
 * {@link hr.tvz.brewlog.brewlogapp.exception.GlobalExceptionHandler}.
 *
 * <p>Filters are disabled ({@code addFilters = false}); {@link WithMockUser} still populates the
 * {@code @AuthenticationPrincipal}. Authorization-rule coverage (role/JWT enforcement) belongs to
 * the dedicated security test suite, not here.
 */
@ControllerSliceTest(CoffeeBeanController.class)
@WithMockUser(username = "barista")
class CoffeeBeanControllerTest {

    private static final String USERNAME = "barista";
    private static final String BASE = "/api/coffeeBeans";

    @Autowired private MockMvc mockMvc;

    /**
     * The slice doesn't load the app's {@code @EnableWebSecurity} config, so the
     * {@code @AuthenticationPrincipal} resolver isn't registered. Register it here so the mock user
     * from {@link WithMockUser} (kept in the {@code SecurityContextHolder}) is injected into controllers.
     */
    @TestConfiguration
    static class PrincipalResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

    @MockitoBean private CoffeeBeanService coffeeBeanService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailServiceImpl userDetailService;

    private static final CoffeeBeanResponse SENTINEL = new CoffeeBeanResponse(
            1L, "Yirgacheffe", "Ethiopia", RoastLevel.MEDIUM, null, "Blue Bottle",
            0.0, 0, null, null, null, null, false, null, false, null, List.of());

    private static final String VALID_CREATE_JSON = """
            {"name":"Yirgacheffe","origin":"Ethiopia","roastLevel":"MEDIUM","roasterName":"Blue Bottle"}""";

    private static final String VALID_UPDATE_JSON = """
            {"name":"Yirgacheffe","origin":"Ethiopia","roastLevel":"MEDIUM","roasterName":"Blue Bottle"}""";

    private static final String BLANK_NAME_JSON = """
            {"name":"  ","origin":"Ethiopia","roastLevel":"MEDIUM","roasterName":"Blue Bottle"}""";

    // ---- create ---


    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        when(coffeeBeanService.createCoffeeBean(any(), eq(USERNAME))).thenReturn(SENTINEL);

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/coffeeBeans/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Yirgacheffe"));
    }

    @Test
    void createDuplicateReturns409WithMessage() throws Exception {
        when(coffeeBeanService.createCoffeeBean(any(), eq(USERNAME)))
                .thenThrow(new DuplicateResourceException("A coffee bean with this name already exists."));

        mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_CREATE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A coffee bean with this name already exists."));
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
        when(coffeeBeanService.removeCoffeeBean(7L, USERNAME)).thenReturn(true);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNoContent());
    }

    @Test
    void deleteReturns404WhenMissing() throws Exception {
        when(coffeeBeanService.removeCoffeeBean(7L, USERNAME)).thenReturn(false);

        mockMvc.perform(delete(BASE + "/7")).andExpect(status().isNotFound());
    }

    @Test
    void deleteWithBrewEntriesReturns409WithMessage() throws Exception {
        when(coffeeBeanService.removeCoffeeBean(7L, USERNAME))
                .thenThrow(new IllegalStateException("Cannot delete coffee bean: it has brew entries."));

        mockMvc.perform(delete(BASE + "/7"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cannot delete coffee bean: it has brew entries."));
    }

    // ---- update ----

    @Test
    void updateReturns200WhenOwned() throws Exception {
        when(coffeeBeanService.updateCoffeeBean(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.of(SENTINEL));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void updateReturns404WhenMissing() throws Exception {
        when(coffeeBeanService.updateCoffeeBean(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.empty());

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRenameCollisionReturns409WithMessage() throws Exception {
        when(coffeeBeanService.updateCoffeeBean(eq(7L), any(), eq(USERNAME)))
                .thenThrow(new DuplicateResourceException("A coffee bean of this roaster with the same name already exists."));

        mockMvc.perform(put(BASE + "/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_UPDATE_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("A coffee bean of this roaster with the same name already exists."));
    }

    // ---- photo upload ----

    @Test
    void uploadPhotoReturns200WhenStored() throws Exception {
        when(coffeeBeanService.uploadPhoto(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.of(SENTINEL));

        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart(BASE + "/7/photo").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void uploadPhotoReturns404WhenBeanMissing() throws Exception {
        when(coffeeBeanService.uploadPhoto(eq(7L), any(), eq(USERNAME))).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
        mockMvc.perform(multipart(BASE + "/7/photo").file(file))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadPhotoBadTypeReturns400WithMessage() throws Exception {
        when(coffeeBeanService.uploadPhoto(eq(7L), any(), eq(USERNAME)))
                .thenThrow(new IllegalArgumentException("Unsupported image type: application/pdf"));

        MockMultipartFile file = new MockMultipartFile("file", "x.pdf", "application/pdf", new byte[]{1});
        mockMvc.perform(multipart(BASE + "/7/photo").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported image type: application/pdf"));
    }

    // ---- flavor-note linking: not-found now maps to 400 (was 500) ----

    @Test
    void addFlavorNoteUnknownReferenceReturns400WithMessage() throws Exception {
        when(coffeeBeanService.addFlavorNote(7L, 3L, USERNAME))
                .thenThrow(new IllegalArgumentException("Coffee bean not found"));

        mockMvc.perform(post(BASE + "/7/flavorNotes/3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Coffee bean not found"));
    }
}
