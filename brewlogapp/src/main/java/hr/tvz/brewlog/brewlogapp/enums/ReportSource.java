package hr.tvz.brewlog.brewlogapp.enums;

/**
 * How a report row came to exist, so clients can tell a scheduler-produced report (worth a
 * "new report" nudge) from one created as a side effect of the user editing a brew (silent).
 */
public enum ReportSource {
    /** Produced by the scheduled job or its startup catch-up/backfill. */
    SCHEDULED,
    /** Created while reconciling reports after the user added/edited/deleted a brew. */
    RECONCILE
}
