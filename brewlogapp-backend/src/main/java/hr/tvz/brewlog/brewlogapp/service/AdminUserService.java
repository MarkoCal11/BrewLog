package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminUserStats;
import hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse;
import hr.tvz.brewlog.brewlogapp.dto.UserResponse;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface AdminUserService {

    List<UserResponse> findAllUsers();

    Page<AuditLogResponse> findAuditLogs(Long targetUserId, Pageable pageable);

    Optional<UserResponse> findUserById(Long id);

    Optional<AdminUserStats> findUserStats(Long id);

    Optional<UserResponse> updateUserRole(Long id, Role role, String actorUsername);

    Optional<UserResponse> deactivateUser(Long id, String actorUsername);

    Optional<UserResponse> reactivateUser(Long id, String actorUsername);
}
