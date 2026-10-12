package hr.tvz.brewlog.brewlogapp.dto;

import hr.tvz.brewlog.brewlogapp.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String actorUsername,
        Long targetUserId,
        String targetUsername,
        AuditAction action,
        String oldValue,
        String newValue,
        LocalDateTime createdAt
) {}
