package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanResponse;
import hr.tvz.brewlog.brewlogapp.dto.CoffeeBeanUpdateRequest;
import hr.tvz.brewlog.brewlogapp.enums.RoastLevel;
import hr.tvz.brewlog.brewlogapp.mapper.CoffeeBeanMapper;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.CoffeeBeanRepository;
import hr.tvz.brewlog.brewlogapp.repository.FlavorNoteRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoffeeBeanServiceImplTest {

    private static final String USERNAME = "barista";

    @Mock private CoffeeBeanRepository coffeeBeanRepository;
    @Mock private CoffeeBeanMapper coffeeBeanMapper;
    @Mock private FlavorNoteRepository flavorNoteRepository;
    @Mock private BrewEntryRepository brewEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks private CoffeeBeanServiceImpl service;

    private User user;

    private static final CoffeeBeanResponse SENTINEL = new CoffeeBeanResponse(
            1L, "name", "origin", RoastLevel.MEDIUM, null, "roaster",
            0.0, 0, null, null, null, null, false, null, false, null, List.of());

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username(USERNAME).build();
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private CoffeeBeanCreateRequest createRequest(String name, String roaster) {
        return new CoffeeBeanCreateRequest(name, "Ethiopia", RoastLevel.MEDIUM, null, roaster,
                null, null, null, null, null);
    }

    private CoffeeBeanUpdateRequest updateRequest(String name, String roaster) {
        return new CoffeeBeanUpdateRequest(name, "Ethiopia", RoastLevel.MEDIUM, null, roaster,
                null, null, null, null, null);
    }

    /** Wire up the shared {@code toResponse} helper path (save echoes, rating/count stubbed). */
    private void stubResponsePipeline() {
        when(coffeeBeanRepository.save(any(CoffeeBean.class))).thenAnswer(inv -> inv.getArgument(0));
        // bean id may be null on the create path, so use any() (anyLong() would not match null)
        when(brewEntryRepository.findAverageRatingByCoffeeId(any())).thenReturn(null);
        when(brewEntryRepository.countByCoffeeBean_Id(any())).thenReturn(0);
        when(coffeeBeanMapper.toResponse(any(CoffeeBean.class), any(), any())).thenReturn(SENTINEL);
    }

    @Nested
    class Create {

        @Test
        void trimsNameAndRoasterAndAssignsUser() {
            CoffeeBeanCreateRequest request = createRequest("  Yirgacheffe  ", "  Blue Bottle  ");
            when(coffeeBeanRepository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(
                    "Yirgacheffe", "Blue Bottle", user)).thenReturn(List.of());
            when(coffeeBeanMapper.toEntity(request)).thenReturn(new CoffeeBean());
            stubResponsePipeline();

            CoffeeBeanResponse result = service.createCoffeeBean(request, USERNAME);

            assertThat(result).isSameAs(SENTINEL);
            ArgumentCaptor<CoffeeBean> beanCaptor = ArgumentCaptor.forClass(CoffeeBean.class);
            ArgumentCaptor<Double> avgCaptor = ArgumentCaptor.forClass(Double.class);
            verify(coffeeBeanMapper).toResponse(beanCaptor.capture(), avgCaptor.capture(), anyInt());
            CoffeeBean saved = beanCaptor.getValue();
            assertThat(saved.getName()).isEqualTo("Yirgacheffe");
            assertThat(saved.getRoasterName()).isEqualTo("Blue Bottle");
            assertThat(saved.getUser()).isSameAs(user);
            // null average from the repository is coalesced to 0.0 before mapping
            assertThat(avgCaptor.getValue()).isEqualTo(0.0);
        }

        @Test
        void rejectsDuplicateNameRoasterPair() {
            CoffeeBeanCreateRequest request = createRequest("Yirgacheffe", "Blue Bottle");
            when(coffeeBeanRepository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser(
                    "Yirgacheffe", "Blue Bottle", user)).thenReturn(List.of(new CoffeeBean()));

            assertThatThrownBy(() -> service.createCoffeeBean(request, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(coffeeBeanRepository, never()).save(any());
        }
    }

    @Nested
    class Remove {

        @Test
        void returnsFalseWhenBeanMissing() {
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

            assertThat(service.removeCoffeeBean(7L, USERNAME)).isFalse();
            verify(coffeeBeanRepository, never()).deleteById(any());
        }

        @Test
        void rejectsDeleteWhenBrewEntriesExist() {
            CoffeeBean bean = CoffeeBean.builder().id(7L).build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            when(brewEntryRepository.existsByCoffeeBean_Id(7L)).thenReturn(true);

            assertThatThrownBy(() -> service.removeCoffeeBean(7L, USERNAME))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("brew entries");

            verify(coffeeBeanRepository, never()).deleteById(any());
        }

        @Test
        void deletesWhenNoBrewEntries() {
            CoffeeBean bean = CoffeeBean.builder().id(7L).build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            when(brewEntryRepository.existsByCoffeeBean_Id(7L)).thenReturn(false);

            assertThat(service.removeCoffeeBean(7L, USERNAME)).isTrue();
            verify(coffeeBeanRepository).deleteById(7L);
        }
    }

    @Nested
    class FlavorNoteLinking {

        @Test
        void addThrowsWhenBeanNotOwned() {
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addFlavorNote(7L, 3L, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Coffee bean not found");
        }

        @Test
        void addThrowsWhenFlavorNoteNotVisible() {
            CoffeeBean bean = CoffeeBean.builder().id(7L).build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            when(flavorNoteRepository.findByIdVisibleToUser(3L, user)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addFlavorNote(7L, 3L, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Flavor note not found");
        }

        @Test
        void addAttachesNoteToBean() {
            CoffeeBean bean = new CoffeeBean();   // no-arg ctor initializes flavorNotes to an empty set
            bean.setId(7L);
            FlavorNote note = FlavorNote.builder().id(3L).name("Honey").build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            when(flavorNoteRepository.findByIdVisibleToUser(3L, user)).thenReturn(Optional.of(note));
            stubResponsePipeline();

            Optional<CoffeeBeanResponse> result = service.addFlavorNote(7L, 3L, USERNAME);

            assertThat(result).contains(SENTINEL);
            assertThat(bean.getFlavorNotes()).contains(note);
        }
    }

    @Nested
    class Update {

        @Test
        void returnsEmptyWhenBeanMissing() {
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

            assertThat(service.updateCoffeeBean(7L, updateRequest("X", "Y"), USERNAME)).isEmpty();
            verify(coffeeBeanRepository, never()).save(any());
        }

        @Test
        void rejectsRenameCollidingWithAnotherBean() {
            CoffeeBean existing = CoffeeBean.builder().id(7L).name("Old").roasterName("Roaster").build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(existing));
            when(coffeeBeanRepository.findByNameIgnoreCaseAndRoasterNameIgnoreCaseAndUser("New", "Roaster", user))
                    .thenReturn(List.of(CoffeeBean.builder().id(99L).build()));

            assertThatThrownBy(() -> service.updateCoffeeBean(7L, updateRequest("  New  ", "  Roaster  "), USERNAME))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(coffeeBeanRepository, never()).save(any());
        }
    }

    @Nested
    class Photo {

        @Test
        void uploadStoresNewPhotoAndDeletesOld() {
            CoffeeBean bean = CoffeeBean.builder().id(7L).photoUrl("/uploads/coffee-beans/old.jpg").build();
            MultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            when(fileStorageService.store(file, "coffee-beans")).thenReturn("/uploads/coffee-beans/new.png");
            stubResponsePipeline();

            Optional<CoffeeBeanResponse> result = service.uploadPhoto(7L, file, USERNAME);

            assertThat(result).contains(SENTINEL);
            assertThat(bean.getPhotoUrl()).isEqualTo("/uploads/coffee-beans/new.png");
            verify(fileStorageService).delete("/uploads/coffee-beans/old.jpg");
        }

        @Test
        void deleteClearsPhotoUrlAndRemovesOldFile() {
            CoffeeBean bean = CoffeeBean.builder().id(7L).photoUrl("/uploads/coffee-beans/old.jpg").build();
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(bean));
            stubResponsePipeline();

            service.deletePhoto(7L, USERNAME);

            assertThat(bean.getPhotoUrl()).isNull();
            verify(fileStorageService).delete("/uploads/coffee-beans/old.jpg");
            verify(fileStorageService, never()).store(any(), any());
        }

        @Test
        void uploadReturnsEmptyWhenBeanMissing() {
            MultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[]{1});
            when(coffeeBeanRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

            assertThat(service.uploadPhoto(7L, file, USERNAME)).isEmpty();
            verify(fileStorageService, never()).store(any(), any());
        }
    }
}
