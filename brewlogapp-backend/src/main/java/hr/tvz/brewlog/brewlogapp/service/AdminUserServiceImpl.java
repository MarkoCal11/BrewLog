package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminUserStats;
import hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuditLogRepository auditLogRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;
    private final BrewEntryRepository brewEntryRepository;
    private final CafeLocationRepository cafeLocationRepository;

    @Override
    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> findAuditLogs(Long targetUserId, Pageable pageable) {
        return auditLogRepository.findAllProjected(targetUserId, pageable);
    }

    @Override
    public Optional<UserResponse> findUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AdminUserStats> findUserStats(Long id) {
        return userRepository.findById(id)
                .map(user -> new AdminUserStats(
                        coffeeBeanRepository.countByUser_Id(user.getId()),
                        brewEntryRepository.countByUser_Id(user.getId()),
                        cafeLocationRepository.countByUser_Id(user.getId())));
    }

    @Override
    public Optional<UserResponse> updateUserRole(Long id, Role role, String actorUsername) {
        return userRepository.findById(id)
                .map(user -> {
                    if (user.getRole() == Role.ADMIN && role != Role.ADMIN) {
                        assertNotLastActiveAdmin(user);
                    }
                    Role oldRole = user.getRole();
                    user.setRole(role);
                    UserResponse response = userMapper.toResponse(userRepository.save(user));
                    audit(actorUsername, user.getId(), AuditAction.ROLE_CHANGE,
                            oldRole == null ? null : oldRole.name(), role.name());
                    return response;
                });
    }

    @Override
    public Optional<UserResponse> deactivateUser(Long id, String actorUsername) {
        return userRepository.findById(id)
                .map(user -> {
                    assertNotLastActiveAdmin(user);
                    boolean oldActive = user.isActive();
                    user.setActive(false);
                    UserResponse response = userMapper.toResponse(userRepository.save(user));
                    audit(actorUsername, user.getId(), AuditAction.DEACTIVATE,
                            String.valueOf(oldActive), "false");
                    return response;
                });
    }

    @Override
    public Optional<UserResponse> reactivateUser(Long id, String actorUsername) {
        return userRepository.findById(id)
                .map(user -> {
                    boolean oldActive = user.isActive();
                    user.setActive(true);
                    UserResponse response = userMapper.toResponse(userRepository.save(user));
                    audit(actorUsername, user.getId(), AuditAction.REACTIVATE,
                            String.valueOf(oldActive), "true");
                    return response;
                });
    }

    private void audit(String actorUsername, Long targetUserId, AuditAction action,
                       String oldValue, String newValue) {
        auditLogRepository.save(AuditLog.builder()
                .actorUsername(actorUsername)
                .targetUserId(targetUserId)
                .action(action)
                .oldValue(oldValue)
                .newValue(newValue)
                .build());
    }

    private void assertNotLastActiveAdmin(User target) {
        if(target.getRole() == Role.ADMIN
        && target.isActive()
        && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new IllegalStateException("Cannot deactivate or demote the last active admin.");
        }
    }
}
