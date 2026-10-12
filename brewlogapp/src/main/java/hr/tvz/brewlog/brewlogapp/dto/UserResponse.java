package hr.tvz.brewlog.brewlogapp.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean active,
        LocalDateTime createdAt
) {}
