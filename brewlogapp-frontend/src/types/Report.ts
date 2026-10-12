// A periodic brewing report generated server-side by the Quartz scheduler. Two scopes
// share this shape: a user's personal report (/api/reports), and the admin's system-wide
// aggregate report rolling up ALL users (/api/admin/reports) — the latter carries
// `activeUsers` and has no per-user owner. The frontend is a pure reader: reports are
// created/persisted by the backend job, never by the app.

export type ReportPeriod = 'WEEKLY' | 'MONTHLY'

// How a report came to exist. SCHEDULED = produced by the Quartz job or its startup
// catch-up/backfill (a genuine "new report arrived"). RECONCILE = created as a side
// effect of the user adding/editing/deleting a brew (e.g. back-dating into a completed
// period) — never a notify-worthy event. Older rows predating this field are null;
// treat null as "not RECONCILE" (i.e. notify), the safe default.
export type ReportSource = 'SCHEDULED' | 'RECONCILE'

/** One bar in the "brews by method" breakdown. `method` is the brew Method enum value. */
export interface MethodCount {
    method: string
    count: number
}

/** One point in the per-day brew-count series (date is ISO YYYY-MM-DD). */
export interface DailyCount {
    date: string
    count: number
}

/** One point in the per-day average-rating series; avgRating null on days with no rated brews. */
export interface DailyRating {
    date: string
    avgRating: number | null
}

export interface Report {
    id: number
    periodType: ReportPeriod
    // Inclusive period bounds, ISO YYYY-MM-DD.
    periodStart: string
    periodEnd: string
    // When the Quartz job produced this report, ISO datetime.
    generatedAt: string
    // Origin of the report (see ReportSource). null on rows created before the backend
    // started recording this; treated as SCHEDULED-equivalent (notify) by the frontend.
    source: ReportSource | null

    // --- headline numbers ---
    brewCount: number
    averageRating: number | null
    topBeanName: string | null
    mostUsedMethod: string | null
    totalDoseGrams: number | null

    // Only on the admin system-wide report: distinct users with ≥1 brew in the period.
    // Absent (undefined) on a personal report.
    activeUsers?: number

    // --- chart series ---
    brewsPerDay: DailyCount[]
    methodBreakdown: MethodCount[]
    ratingTrend: DailyRating[]
}

/** Parse an ISO YYYY-MM-DD as a *local* date (avoids the UTC off-by-one from `new Date(str)`). */
function localDate(iso: string): Date {
    const [y, m, d] = iso.split('-').map(Number)
    return new Date(y, m - 1, d)
}

/**
 * Human label for a report's period: month name for MONTHLY, a date range for WEEKLY.
 * e.g. "June 2026" or "Jun 8 – 14, 2026".
 */
export function formatPeriodLabel(report: Pick<Report, 'periodType' | 'periodStart' | 'periodEnd'>): string {
    if (report.periodType === 'MONTHLY') {
        return localDate(report.periodStart).toLocaleDateString(undefined, { month: 'long', year: 'numeric' })
    }
    // Use Intl's range formatter so it collapses shared month/year correctly in any
    // locale (building the range from two partial dates breaks under non-US orderings).
    const fmt = new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', year: 'numeric' })
    return fmt.formatRange(localDate(report.periodStart), localDate(report.periodEnd))
}

/** Short axis/label form of an ISO date, e.g. "6/8". */
export function shortDay(iso: string): string {
    return localDate(iso).toLocaleDateString(undefined, { month: 'numeric', day: 'numeric' })
}

/** Readable "generated at" timestamp. */
export function formatGeneratedAt(iso: string): string {
    return new Date(iso).toLocaleString(undefined, {
        year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
    })
}

/** Title-case a period type for display ("WEEKLY" → "Weekly"). */
export function formatPeriodType(period: ReportPeriod): string {
    return period.charAt(0) + period.slice(1).toLowerCase()
}
