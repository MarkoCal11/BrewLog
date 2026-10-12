package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.CafeLocationCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CafeLocationResponse;
import hr.tvz.brewlog.brewlogapp.mapper.CafeLocationMapper;
import hr.tvz.brewlog.brewlogapp.model.CafeLocation;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.CafeLocationRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CafeLocationServiceImplTest {

    private static final String USERNAME = "barista";

    @Mock private CafeLocationRepository cafeLocationRepository;
    @Mock private CafeLocationMapper cafeLocationMapper;
    @Mock private UserRepository userRepository;

    @InjectMocks private CafeLocationServiceImpl service;

    private User user;

    private static final CafeLocationResponse SENTINEL =
            new CafeLocationResponse(1L, "name", null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username(USERNAME).build();
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    /** Runs createCafeLocation with a mapper that yields the given raw entity, returns what was saved. */
    private CafeLocation createAndCaptureSaved(CafeLocation rawEntity) {
        when(cafeLocationMapper.toEntity(any())).thenReturn(rawEntity);
        when(cafeLocationRepository.save(any(CafeLocation.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cafeLocationMapper.toResponse(any(CafeLocation.class))).thenReturn(SENTINEL);

        CafeLocationCreateRequest request =
                new CafeLocationCreateRequest("ignored", null, null, null, null, null, null);
        service.createCafeLocation(request, USERNAME);

        ArgumentCaptor<CafeLocation> captor = ArgumentCaptor.forClass(CafeLocation.class);
        org.mockito.Mockito.verify(cafeLocationRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void createAssignsUserAndTrimsBlankFieldsToNull() {
        CafeLocation raw = CafeLocation.builder()
                .name("  Café Noir  ")
                .city("   ")
                .address("  12 Main St  ")
                .build();

        CafeLocation saved = createAndCaptureSaved(raw);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo("Café Noir");
        assertThat(saved.getCity()).isNull();             // blank -> null
        assertThat(saved.getAddress()).isEqualTo("12 Main St");
    }

    @ParameterizedTest
    @CsvSource(value = {
            "@CoolCafe                                  | coolcafe",
            "CoolCafe                                   | coolcafe",
            "https://instagram.com/@CoolCafe/           | coolcafe",
            "https://www.instagram.com/CoolCafe         | coolcafe",
            "coolcafe?igshid=abc                        | coolcafe",
            "coolcafe/posts                             | coolcafe",
            "'   '                                      | ",
    }, delimiter = '|')
    void normalizesInstagramHandle(String input, String expected) {
        CafeLocation raw = CafeLocation.builder().name("Cafe").instagram(input.trim()).build();

        CafeLocation saved = createAndCaptureSaved(raw);

        // expected is null when the CSV cell is blank
        assertThat(saved.getInstagram()).isEqualTo(expected == null ? null : expected.trim());
    }

    @Test
    void updateReturnsEmptyWhenNotOwned() {
        when(cafeLocationRepository.findByIdAndUser(9L, user)).thenReturn(Optional.empty());

        Optional<CafeLocationResponse> result = service.updateCafeLocation(
                9L, new hr.tvz.brewlog.brewlogapp.dto.CafeLocationUpdateRequest(
                        "X", null, null, null, null, null, null), USERNAME);

        assertThat(result).isEmpty();
    }
}
