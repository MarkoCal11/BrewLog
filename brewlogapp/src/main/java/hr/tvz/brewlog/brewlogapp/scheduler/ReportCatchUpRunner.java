package hr.tvz.brewlog.brewlogapp.scheduler;

import hr.tvz.brewlog.brewlogapp.enums.PeriodType;
import hr.tvz.brewlog.brewlogapp.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Recovers report firings that were due while the app was down. The Quartz scheduler uses an
 * in-memory job store, so a cron trigger that elapses while the process is stopped is simply lost —
 * it is never back-filled on restart. This runner fills that gap: once the context is ready, it
 * generates the previous period's reports for any that don't already exist, so a user sees the
 * missing weekly/monthly report as soon as they reopen the app.
 *
 * <p>The walk covers every missing window back to the earliest brew, so an outage spanning several
 * windows is fully recovered, not just the most recent one.
 */
@Component
public class ReportCatchUpRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportCatchUpRunner.class);

    private final ReportService reportService;

    public ReportCatchUpRunner(ReportService reportService) {
        this.reportService = reportService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpMissedReports() {
        for (PeriodType periodType : PeriodType.values()) {
            log.info("Report catch-up check on startup for {}", periodType);
            reportService.backfillMissing(periodType);
            reportService.backfillAggregateMissing(periodType);
        }
    }
}
