package hr.tvz.brewlog.brewlogapp.controller;

import hr.tvz.brewlog.brewlogapp.dto.AdminUserStats;
import hr.tvz.brewlog.brewlogapp.dto.UserResponse;
import hr.tvz.brewlog.brewlogapp.dto.UserRoleUpdateRequest;
import hr.tvz.brewlog.brewlogapp.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(adminUserService.findAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return adminUserService.findUserById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<AdminUserStats> getUserStats(@PathVariable Long id) {
        return adminUserService.findUserStats(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<UserResponse> updateUserRole(@PathVariable Long id,
                                                       @Valid @RequestBody UserRoleUpdateRequest request,
                                                       @AuthenticationPrincipal UserDetails actor) {
        return adminUserService.updateUserRole(id, request.role(), actor.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails actor) {
        return adminUserService.deactivateUser(id, actor.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/{id}/reactivate")
    public ResponseEntity<UserResponse> reactivateUser(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserDetails actor) {
        return adminUserService.reactivateUser(id, actor.getUsername())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
