package hr.tvz.brewlog.brewlogapp.dto;

public record AuthUserResponse(Long id, String username, String role, String displayName, String firstName, String lastName) {}
