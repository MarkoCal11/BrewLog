package hr.tvz.brewlog.brewlogapp.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password,
        boolean rememberMe
) {}
