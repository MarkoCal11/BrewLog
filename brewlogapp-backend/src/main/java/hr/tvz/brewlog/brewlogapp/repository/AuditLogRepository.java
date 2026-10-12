package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse;
import hr.tvz.brewlog.brewlogapp.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query(value = """
            SELECT new hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse(
                a.id, a.actorUsername, a.targetUserId, u.username,
                a.action, a.oldValue, a.newValue, a.createdAt)
            FROM AuditLog a
            LEFT JOIN User u ON u.id = a.targetUserId
            WHERE (:targetUserId IS NULL OR a.targetUserId = :targetUserId)
            ORDER BY a.createdAt DESC
            """,
            countQuery = """
            SELECT count(a) FROM AuditLog a
            WHERE (:targetUserId IS NULL OR a.targetUserId = :targetUserId)
            """)
    Page<AuditLogResponse> findAllProjected(@Param("targetUserId") Long targetUserId, Pageable pageable);
}
