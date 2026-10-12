package hr.tvz.brewlog.brewlogapp.service;

import hr.tvz.brewlog.brewlogapp.dto.AdminReportResponse;
import hr.tvz.brewlog.brewlogapp.dto.ReportResponse;
import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.model.User;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportService {

    List<ReportResponse> findAllReports(String username);

    Optional<ReportResponse> findReportById(Long id, String username);

    List<AdminReportResponse> findAllReportsGlobal();

    Optional<AdminReportResponse> findReportByIdGlobal(Long id);

    void generatePrevious(PeriodType periodType);

    void generateAggregatePrevious(PeriodType periodType);

    /**
     * Generates the previous period's per-user reports only for users who don't already have one,
     * to recover firings missed while the app was down. Existing reports are left untouched (no
     * recompute, no duplicate nudge), so this is safe to run on every startup. Only the single most
     * recent completed window is covered.
     */
    void generatePreviousIfMissing(PeriodType periodType);

    /**
     * Generates the previous period's aggregate report only if it doesn't already exist — the
     * aggregate counterpart to {@link #generatePreviousIfMissing(PeriodType)} for startup catch-up.
     */
    void generateAggregatePreviousIfMissing(PeriodType periodType);

    /**
     * Per-user catch-up that walks backward from the previous window to the window containing the
     * earliest brew, generating any missing report along the way. Recovers an outage spanning several
     * windows, not just the most recent. Existing reports are left untouched; only the most recent
     * window emits "new report" nudges, older back-filled reports are surfaced silently.
     */
    void backfillMissing(PeriodType periodType);

    /**
     * Aggregate counterpart to {@link #backfillMissing(PeriodType)}: back-fills every missing aggregate
     * report from the previous window back to the earliest brew.
     */
    void backfillAggregateMissing(PeriodType periodType);

    /**
     * Refreshes both {@code user}'s reports and the global aggregate for the period windows containing
     * the given dates, deleting any whose window no longer has brews and recreating a completed
     * period's report when brews return to it. Call after a brew is created, updated, or deleted so
     * reports stay consistent without waiting for the next scheduled generation. The in-progress
     * period is left uncreated — first-time creation there stays the scheduler's job.
     */
    void reconcileReports(User user, Collection<LocalDate> affectedDates);
}
