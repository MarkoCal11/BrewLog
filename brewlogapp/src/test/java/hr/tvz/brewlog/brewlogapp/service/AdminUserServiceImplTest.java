package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminUserStats;
import hr.tvz.brewlog.brewlogapp.dto.UserResponse;
import hr.tvz.brewlog.brewlogapp.enums.AuditAction;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.mapper.UserMapper;
import hr.tvz.brewlog.brewlogapp.model.AuditLog;
import hr.tvz.brewlog.brewlogapp.model.User;
import hr.tvz.brewlog.brewlogapp.repository.AuditLogRepository;
import hr.tvz.brewlog.brewlogapp.repository.BrewEntryRepository;
import hr.tvz.brewlog.brewlogapp.repository.CafeLocationRepository;
import hr.tvz.brewlog.brewlogapp.repository.CoffeeBeanRepository;
import hr.tvz.brewlog.brewlogapp.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    private static final String ACTOR = "admin";

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CoffeeBeanRepository coffeeBeanRepository;
    @Mock private BrewEntryRepository brewEntryRepository;
    @Mock private CafeLocationRepository cafeLocationRepository;

    @InjectMocks private AdminUserServiceImpl service;

    private static final UserResponse SENTINEL =
            new UserResponse(1L, "u", "e@x.com", "USER", true, null);

    private User userWith(Long id, Role role, boolean active) {
        return User.builder().id(id).username("u" + id).role(role).active(active).build();
    }

    private void stubMapper() {
        lenient().when(userMapper.toResponse(any(User.class))).thenReturn(SENTINEL);
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void updateUserRolePromotesAndWritesAudit() {
        User target = userWith(2L, Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubMapper();

        Optional<UserResponse> result = service.updateUserRole(2L, Role.ADMIN, ACTOR);

        assertThat(result).contains(SENTINEL);
        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        AuditLog log = captureAudit();
        assertThat(log.getAction()).isEqualTo(AuditAction.ROLE_CHANGE);
        assertThat(log.getActorUsername()).isEqualTo(ACTOR);
        assertThat(log.getTargetUserId()).isEqualTo(2L);
        assertThat(log.getOldValue()).isEqualTo("USER");
        assertThat(log.getNewValue()).isEqualTo("ADMIN");
    }

    @Test
    void demotingLastActiveAdminIsRejected() {
        User target = userWith(2L, Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.updateUserRole(2L, Role.USER, ACTOR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("last active admin");

        verify(userRepository, never()).save(any());
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void demotingAdminAllowedWhenAnotherAdminExists() {
        User target = userWith(2L, Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(2L);
        stubMapper();

        service.updateUserRole(2L, Role.USER, ACTOR);

        assertThat(target.getRole()).isEqualTo(Role.USER);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void deactivatingLastActiveAdminIsRejected() {
        User target = userWith(2L, Role.ADMIN, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deactivateUser(2L, ACTOR))
                .isInstanceOf(IllegalStateException.class);

        assertThat(target.isActive()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUserFlipsActiveAndAudits() {
        User target = userWith(2L, Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubMapper();

        service.deactivateUser(2L, ACTOR);

        assertThat(target.isActive()).isFalse();
        AuditLog log = captureAudit();
        assertThat(log.getAction()).isEqualTo(AuditAction.DEACTIVATE);
        assertThat(log.getOldValue()).isEqualTo("true");
        assertThat(log.getNewValue()).isEqualTo("false");
    }

    @Test
    void reactivateUserFlipsActiveAndAudits() {
        User target = userWith(2L, Role.USER, false);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        stubMapper();

        service.reactivateUser(2L, ACTOR);

        assertThat(target.isActive()).isTrue();
        AuditLog log = captureAudit();
        assertThat(log.getAction()).isEqualTo(AuditAction.REACTIVATE);
        assertThat(log.getOldValue()).isEqualTo("false");
        assertThat(log.getNewValue()).isEqualTo("true");
    }

    @Test
    void updateUserRoleReturnsEmptyWhenUserMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.updateUserRole(99L, Role.ADMIN, ACTOR)).isEmpty();
        verify(auditLogRepository, never()).save(any());
    }

    @Test
    void findUserStatsAggregatesCounts() {
        User target = userWith(2L, Role.USER, true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(target));
        when(coffeeBeanRepository.countByUser_Id(2L)).thenReturn(3L);
        when(brewEntryRepository.countByUser_Id(2L)).thenReturn(7L);
        when(cafeLocationRepository.countByUser_Id(2L)).thenReturn(2L);

        Optional<AdminUserStats> stats = service.findUserStats(2L);

        assertThat(stats).contains(new AdminUserStats(3L, 7L, 2L));
    }

    private AuditLog captureAudit() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
