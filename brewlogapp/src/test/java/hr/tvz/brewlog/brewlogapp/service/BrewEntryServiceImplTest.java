package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.BrewEntryCreateRequest;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryResponse;
import hr.tvz.brewlog.brewlogapp.dto.BrewEntryUpdateRequest;
import hr.tvz.brewlog.brewlogapp.enums.Method;
import hr.tvz.brewlog.brewlogapp.mapper.BrewEntryMapper;
import hr.tvz.brewlog.brewlogapp.model.BrewEntry;
import hr.tvz.brewlog.brewlogapp.model.CoffeeBean;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.CoffeeBeanRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrewEntryServiceImplTest {

    private static final String USERNAME = "barista";
    private static final LocalDateTime NOW = LocalDateTime.now();


    @Mock private BrewEntryRepository brewEntryRepository;
    @Mock private CoffeeBeanRepository coffeeBeanRepository;
    @Mock private BrewEntryMapper brewEntryMapper;
    @Mock private UserRepository userRepository;
    @Mock private ReportService reportService;

    @InjectMocks private BrewEntryServiceImpl service;

    private User user;

    private static final BrewEntryResponse SENTINEL = new BrewEntryResponse(
            1L, Method.V60, null, null, null, null, null, null, null, null, null, null);

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username(USERNAME).build();
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(user));
    }

    private BrewEntryCreateRequest createRequest(Long coffeeBeanId) {
        return new BrewEntryCreateRequest(Method.V60, null, null, null, 18.0, null, null,
                4.0, null, LocalDateTime.now(), coffeeBeanId);
    }

    @Test
    void createResolvesBeanAndAssignsUser() {
        CoffeeBean bean = CoffeeBean.builder().id(5L).build();
        BrewEntry entity = new BrewEntry();
        entity.setBrewedAt(LocalDateTime.now());
        when(coffeeBeanRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(bean));
        when(brewEntryMapper.toEntity(any())).thenReturn(entity);
        when(brewEntryRepository.save(any(BrewEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(brewEntryMapper.toResponse(any(BrewEntry.class))).thenReturn(SENTINEL);

        BrewEntryResponse result = service.createBrewEntry(createRequest(5L), USERNAME);

        assertThat(result).isSameAs(SENTINEL);
        ArgumentCaptor<BrewEntry> captor = ArgumentCaptor.forClass(BrewEntry.class);
        verify(brewEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getCoffeeBean()).isSameAs(bean);
        assertThat(captor.getValue().getUser()).isSameAs(user);
    }

    @Test
    void createThrowsWhenCoffeeBeanNotOwned() {
        when(coffeeBeanRepository.findByIdAndUser(5L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createBrewEntry(createRequest(5L), USERNAME))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coffee bean not found");

        verify(brewEntryRepository, never()).save(any());
    }

    @Test
    void removeReturnsFalseWhenMissing() {
        when(brewEntryRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

        assertThat(service.removeBrewEntryById(7L, USERNAME)).isFalse();
        verify(brewEntryRepository, never()).deleteById(any());
    }

    @Test
    void removeDeletesWhenOwned() {
        BrewEntry brew = new BrewEntry();
        brew.setBrewedAt(LocalDateTime.now());
        when(brewEntryRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(brew));

        assertThat(service.removeBrewEntryById(7L, USERNAME)).isTrue();
        verify(brewEntryRepository).deleteById(7L);
        // The deleted brew's period report must be reconciled, not left until the next scheduled run.
        verify(reportService).reconcileReports(eq(user), anyCollection());
    }

    @Test
    void updateReturnsEmptyWhenNotOwned() {
        BrewEntryUpdateRequest request = new BrewEntryUpdateRequest(
                Method.V60, null, 94.0, 4.0, null, 18.0, null, null, null, NOW, 5L);
        when(brewEntryRepository.findByIdAndUser(7L, user)).thenReturn(Optional.empty());

        assertThat(service.updateBrewEntry(7L, request, USERNAME)).isEmpty();
        verify(brewEntryRepository, never()).save(any());
    }

    @Test
    void updateAppliesMapperAndSaves() {
        BrewEntry existing = new BrewEntry();
        existing.setBrewedAt(LocalDateTime.now());
        BrewEntryUpdateRequest request = new BrewEntryUpdateRequest(
                Method.V60, null, 94.0, 4.0, null, 18.0, null, null, null, NOW, 5L);
        CoffeeBean bean = new CoffeeBean();
        when(brewEntryRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(existing));
        when(coffeeBeanRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(bean));
        when(brewEntryRepository.save(existing)).thenReturn(existing);
        when(brewEntryMapper.toResponse(existing)).thenReturn(SENTINEL);

        Optional<BrewEntryResponse> result = service.updateBrewEntry(7L, request, USERNAME);

        assertThat(result).contains(SENTINEL);
        verify(brewEntryMapper).updateEntityFromRequest(request, existing);
        verify(brewEntryRepository).save(existing);
    }

    @Test
    void removeGlobalReconcilesAgainstBrewOwner() {
        BrewEntry brew = new BrewEntry();
        brew.setUser(user);
        brew.setBrewedAt(LocalDateTime.now());
        when(brewEntryRepository.findById(7L)).thenReturn(Optional.of(brew));

        assertThat(service.removeBrewEntryByIdGlobal(7L)).isTrue();
        verify(brewEntryRepository).deleteById(7L);
        // admin delete reconciles too — against the brew's owner, not the acting admin
        verify(reportService).reconcileReports(eq(user), anyCollection());
    }

    @Test
    void updateGlobalReconcilesAgainstBrewOwner() {
        BrewEntry existing = new BrewEntry();
        existing.setUser(user);
        existing.setBrewedAt(LocalDateTime.now());
        BrewEntryUpdateRequest request = new BrewEntryUpdateRequest(
                Method.V60, null, 94.0, 4.0, null, 18.0, null, null, null, NOW, 5L);
        CoffeeBean bean = new CoffeeBean();
        when(brewEntryRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(coffeeBeanRepository.findByIdAndUser(5L, user)).thenReturn(Optional.of(bean));
        when(brewEntryRepository.save(existing)).thenReturn(existing);

        service.updateBrewEntryGlobal(7L, request);

        verify(brewEntryRepository).save(existing);
        verify(reportService).reconcileReports(eq(user), anyCollection());
    }
}
