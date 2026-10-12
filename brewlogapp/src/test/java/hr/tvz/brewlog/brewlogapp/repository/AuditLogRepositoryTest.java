package hr.tvz.brewlog.brewlogapp.repository;

import hr.tvz.brewlog.brewlogapp.dto.AuditLogResponse;
import hr.tvz.brewlog.brewlogapp.enums.AuditAction;
import hr.tvz.brewlog.brewlogapp.enums.Role;
import hr.tvz.brewlog.brewlogapp.model.AuditLog;
import hr.tvz.brewlog.brewlogapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test for {@link AuditLogRepository#findAllProjected} — the constructor-projection
 * JPQL with an optional {@code targetUserId} filter, a {@code LEFT JOIN User} to resolve the target's
 * username (null when no matching user), pagination, and DESC ordering by {@code createdAt}.
 */
@RepositorySliceTest
class AuditLogRepositoryTest {

    private static final long ORPHAN_TARGET_ID = 9_999L;

    @Autowired private AuditLogRepository repository;
    @Autowired private TestEntityManager em;

    private Long victimId;

    @BeforeEach
    void setUp() {
        User victim = em.persist(User.builder().username("victim").password("x").email("v@x.com")
                .role(Role.USER).active(true).build());
        victimId = victim.getId();
    }

    private void log(Long targetUserId, AuditAction action) {
        em.persist(AuditLog.builder()
                .actorUsername("admin").targetUserId(targetUserId).action(action)
                .oldValue("USER").newValue("ADMIN").build());
    }

    @Test
    void nullFilterReturnsAllProjectedWithResolvedUsername() {
        log(victimId, AuditAction.ROLE_CHANGE);
        log(ORPHAN_TARGET_ID, AuditAction.DEACTIVATE);
        em.flush();
        em.clear();

        Page<AuditLogResponse> page = repository.findAllProjected(null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .anySatisfy(r -> {
                    assertThat(r.targetUserId()).isEqualTo(victimId);
                    assertThat(r.targetUsername()).isEqualTo("victim");
                    assertThat(r.actorUsername()).isEqualTo("admin");
                })
                .anySatisfy(r -> {
                    assertThat(r.targetUserId()).isEqualTo(ORPHAN_TARGET_ID);
                    assertThat(r.targetUsername()).isNull();   // LEFT JOIN, no matching user
                });
    }

    @Test
    void filtersByTargetUserId() {
        log(victimId, AuditAction.ROLE_CHANGE);
        log(ORPHAN_TARGET_ID, AuditAction.DEACTIVATE);
        em.flush();
        em.clear();

        Page<AuditLogResponse> page = repository.findAllProjected(victimId, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(AuditLogResponse::targetUserId)
                .containsExactly(victimId);
    }

    @Test
    void paginatesResults() {
        log(victimId, AuditAction.ROLE_CHANGE);
        log(victimId, AuditAction.DEACTIVATE);
        log(victimId, AuditAction.REACTIVATE);
        em.flush();
        em.clear();

        Page<AuditLogResponse> firstPage = repository.findAllProjected(victimId, PageRequest.of(0, 2));

        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent()).hasSize(2);
    }

    @Test
    void ordersByCreatedAtDescending() {
        log(victimId, AuditAction.ROLE_CHANGE);
        log(victimId, AuditAction.DEACTIVATE);
        log(victimId, AuditAction.REACTIVATE);
        em.flush();
        em.clear();

        Page<AuditLogResponse> page = repository.findAllProjected(victimId, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(AuditLogResponse::createdAt)
                .isSortedAccordingTo(Comparator.nullsLast(Comparator.reverseOrder()));
    }
}
