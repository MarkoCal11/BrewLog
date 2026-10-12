package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteResponse;
import hr.tvz.brewlog.brewlogapp.dto.FlavorNoteUpdateRequest;
import hr.tvz.brewlog.brewlogapp.enums.Category;
import hr.tvz.brewlog.brewlogapp.mapper.FlavorNoteMapper;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.FlavorNote;
import hr.tvz.brewlog.brewlogapp.model.User;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlavorNoteServiceImplTest {

    private static final String USERNAME = "barista";

    @Mock
    private FlavorNoteRepository flavorNoteRepository;

    @Mock
    private FlavorNoteMapper flavorNoteMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FlavorNoteServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username(USERNAME).build();
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private FlavorNote flavorNote(Long id, String name, Category category, User owner) {
        return FlavorNote.builder().id(id).name(name).category(category).user(owner).build();
    }

    private FlavorNoteResponse responseFor(FlavorNote note) {
        return new FlavorNoteResponse(note.getId(), note.getName(), note.getCategory(), note.getUser() == null);
    }

    @Nested
    class UserResolution {

        @Test
        void throwsWhenUserNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findAllFlavorNotes("ghost"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("ghost");
        }
    }

    @Nested
    class Create {

        @Test
        void trimsNameAndAssignsUser() {
            FlavorNoteCreateRequest request = new FlavorNoteCreateRequest("  Honey  ", Category.SWEET);
            FlavorNote mapped = flavorNote(null, "  Honey  ", Category.SWEET, null);
            when(flavorNoteMapper.toEntity(request)).thenReturn(mapped);
            when(flavorNoteRepository.findByNameIgnoreCaseAndUser("Honey", user)).thenReturn(Optional.empty());
            when(flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull("Honey")).thenReturn(Optional.empty());
            when(flavorNoteRepository.save(any(FlavorNote.class))).thenAnswer(inv -> inv.getArgument(0));
            when(flavorNoteMapper.toResponse(any(FlavorNote.class))).thenAnswer(inv -> responseFor(inv.getArgument(0)));

            FlavorNoteResponse result = service.createFlavorNote(request, USERNAME);

            ArgumentCaptor<FlavorNote> captor = ArgumentCaptor.forClass(FlavorNote.class);
            verify(flavorNoteRepository).save(captor.capture());
            FlavorNote saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Honey");
            assertThat(saved.getUser()).isSameAs(user);
            assertThat(result.name()).isEqualTo("Honey");
            assertThat(result.global()).isFalse();
        }

        @Test
        void rejectsDuplicateOwnedName() {
            FlavorNoteCreateRequest request = new FlavorNoteCreateRequest("Honey", Category.SWEET);
            when(flavorNoteRepository.findByNameIgnoreCaseAndUser("Honey", user))
                    .thenReturn(Optional.of(flavorNote(2L, "Honey", Category.SWEET, user)));

            assertThatThrownBy(() -> service.createFlavorNote(request, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already exists");

            verify(flavorNoteRepository, never()).save(any());
        }

        @Test
        void rejectsNameCollidingWithGlobalNote() {
            FlavorNoteCreateRequest request = new FlavorNoteCreateRequest("Honey", Category.SWEET);
            when(flavorNoteRepository.findByNameIgnoreCaseAndUser("Honey", user)).thenReturn(Optional.empty());
            when(flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull("Honey"))
                    .thenReturn(Optional.of(flavorNote(3L, "Honey", Category.SWEET, null)));

            assertThatThrownBy(() -> service.createFlavorNote(request, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(flavorNoteRepository, never()).save(any());
        }
    }

    @Nested
    class Update {

        @Test
        void returnsEmptyWhenNoteNotOwnedByUser() {
            when(flavorNoteRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

            Optional<FlavorNoteResponse> result =
                    service.updateFlavorNote(99L, new FlavorNoteUpdateRequest("X", Category.SOUR), USERNAME);

            assertThat(result).isEmpty();
            verify(flavorNoteRepository, never()).save(any());
        }

        @Test
        void rejectsRenameCollidingWithAnotherOwnedNote() {
            FlavorNote existing = flavorNote(5L, "Honey", Category.SWEET, user);
            when(flavorNoteRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(existing));
            when(flavorNoteRepository.findByNameIgnoreCaseAndUser("Caramel", user))
                    .thenReturn(Optional.of(flavorNote(6L, "Caramel", Category.SWEET, user)));

            FlavorNoteUpdateRequest request = new FlavorNoteUpdateRequest("  Caramel  ", Category.SWEET);
            assertThatThrownBy(() -> service.updateFlavorNote(5L, request, USERNAME))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(flavorNoteRepository, never()).save(any());
        }

        @Test
        void allowsKeepingSameNameDifferentCasing() {
            FlavorNote existing = flavorNote(5L, "Honey", Category.SWEET, user);
            when(flavorNoteRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(existing));
            when(flavorNoteRepository.save(any(FlavorNote.class))).thenAnswer(inv -> inv.getArgument(0));
            when(flavorNoteMapper.toResponse(any(FlavorNote.class))).thenAnswer(inv -> responseFor(inv.getArgument(0)));

            Optional<FlavorNoteResponse> result =
                    service.updateFlavorNote(5L, new FlavorNoteUpdateRequest("honey", Category.SWEET), USERNAME);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("honey");
            verify(flavorNoteRepository, never()).findByNameIgnoreCaseAndUser("honey", user);
        }
    }

    @Nested
    class Remove {

        @Test
        void returnsFalseWhenNoteMissing() {
            when(flavorNoteRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

            assertThat(service.removeFlavorNote(7L, USERNAME)).isFalse();
            verify(flavorNoteRepository, never()).delete(any());
        }

        @Test
        void detachesFromCoffeeBeansBeforeDeleting() {
            FlavorNote note = flavorNote(7L, "Honey", Category.SWEET, user);
            CoffeeBean bean = new CoffeeBean();
            Set<FlavorNote> beanNotes = new HashSet<>();
            beanNotes.add(note);
            bean.setFlavorNotes(beanNotes);
            note.setCoffeeBean(new HashSet<>(Set.of(bean)));
            when(flavorNoteRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(note));

            assertThat(service.removeFlavorNote(7L, USERNAME)).isTrue();

            assertThat(bean.getFlavorNotes()).doesNotContain(note);
            verify(flavorNoteRepository).delete(note);
        }
    }

    @Nested
    class GlobalNotes {

        @Test
        void createGlobalSetsNullOwnerAndTrims() {
            FlavorNoteCreateRequest request = new FlavorNoteCreateRequest("  Oak  ", Category.HERBAL);
            FlavorNote mapped = flavorNote(null, "  Oak  ", Category.HERBAL, user);
            when(flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull("Oak")).thenReturn(Optional.empty());
            when(flavorNoteMapper.toEntity(request)).thenReturn(mapped);
            when(flavorNoteRepository.save(any(FlavorNote.class))).thenAnswer(inv -> inv.getArgument(0));
            when(flavorNoteMapper.toResponse(any(FlavorNote.class))).thenAnswer(inv -> responseFor(inv.getArgument(0)));

            FlavorNoteResponse result = service.createFlavorNoteGlobal(request);

            ArgumentCaptor<FlavorNote> captor = ArgumentCaptor.forClass(FlavorNote.class);
            verify(flavorNoteRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Oak");
            assertThat(captor.getValue().getUser()).isNull();
            assertThat(result.global()).isTrue();
        }

        @Test
        void createGlobalRejectsDuplicate() {
            FlavorNoteCreateRequest request = new FlavorNoteCreateRequest("Oak", Category.HERBAL);
            when(flavorNoteRepository.findByNameIgnoreCaseAndUserIsNull("Oak"))
                    .thenReturn(Optional.of(flavorNote(8L, "Oak", Category.HERBAL, null)));

            assertThatThrownBy(() -> service.createFlavorNoteGlobal(request))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(flavorNoteRepository, never()).save(any());
        }
    }

    @Nested
    class Queries {

        @Test
        void findAllMapsEveryVisibleNote() {
            List<FlavorNote> notes = List.of(
                    flavorNote(1L, "Honey", Category.SWEET, user),
                    flavorNote(2L, "Oak", Category.HERBAL, null));
            when(flavorNoteRepository.findAllVisibleToUser(user)).thenReturn(notes);
            when(flavorNoteMapper.toResponse(any(FlavorNote.class))).thenAnswer(inv -> responseFor(inv.getArgument(0)));

            List<FlavorNoteResponse> result = service.findAllFlavorNotes(USERNAME);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(FlavorNoteResponse::name).containsExactly("Honey", "Oak");
            assertThat(result).extracting(FlavorNoteResponse::global).containsExactly(false, true);
        }
    }
}
