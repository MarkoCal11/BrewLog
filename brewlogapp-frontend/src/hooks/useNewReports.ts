import { useCallback, useEffect, useMemo, useState } from 'react'
import { useReports } from './useReports'
import { useAdminReports } from './useAdminReports'
import { useAuth } from '../context/AuthContext'
import type { Report, ReportPeriod } from '../types/Report'

// Reports are generated server-side (Quartz), so "new" is purely client-side: we track
// the highest report id this user has already seen on this device. Anything with a
// larger id is a freshly-generated report we haven't surfaced yet. Personal and admin
// (system-wide) reports get separate marker keys so their banners never clear each other.
const personalKey = (userId: number) => `brewlog:reports:lastSeenId:${userId}`
const adminKey = (userId: number) => `brewlog:adminReports:lastSeenId:${userId}`

function readMarker(key: string): number | null {
    const raw = localStorage.getItem(key)
    if (raw == null) return null
    const n = Number(raw)
    return Number.isFinite(n) ? n : null
}

function writeMarker(key: string, id: number) {
    localStorage.setItem(key, String(id))
}

type UseNewReportsResult = {
    // Newest unseen report per period type (absent if none new for that period).
    newByPeriod: Partial<Record<ReportPeriod, Report>>
    // Clear the alerts: mark every currently-loaded report as seen.
    markAllSeen: () => void
}

/**
 * Shared engine for the "new report arrived" banner: given the loaded reports and a
 * per-user localStorage key builder, surface the newest unseen report per period type
 * plus a "mark all seen" action. `keyFor` must be a stable module-level function.
 */
function useNewReportsCore(
    data: Report[] | null | undefined,
    keyFor: (userId: number) => string,
): UseNewReportsResult {
    const { user } = useAuth()
    const userId = user?.id ?? null
    const [marker, setMarker] = useState<number | null>(null)

    // Auth rehydrates asynchronously on reload, so userId is null on the first render.
    // Re-read the stored marker once we actually know who's logged in (and reset it on
    // logout / user switch) — otherwise the in-memory marker stays stale and the banner
    // never renders for a returning user.
    useEffect(() => {
        setMarker(userId != null ? readMarker(keyFor(userId)) : null)
    }, [userId, keyFor])

    // Only the newest unseen report per period type can ever show (≤ 2 banners), so we
    // don't suppress on first visit: an unset marker (null) means "nothing seen yet".
    // RECONCILE reports (created as a side effect of the user's own brew add/edit/delete)
    // are never notify-worthy, so they can't raise a banner. They still count toward the
    // marker via markAllSeen below, so a dismissed RECONCILE row can't resurface.
    const newByPeriod = useMemo(() => {
        const out: Partial<Record<ReportPeriod, Report>> = {}
        if (!data) return out
        const threshold = marker ?? 0
        for (const r of data) {
            if (r.id <= threshold) continue
            if (r.source === 'RECONCILE') continue
            const current = out[r.periodType]
            if (!current || r.id > current.id) out[r.periodType] = r
        }
        return out
    }, [data, marker])

    const markAllSeen = useCallback(() => {
        if (userId == null || !data || data.length === 0) return
        const maxId = Math.max(...data.map(r => r.id))
        writeMarker(keyFor(userId), maxId)
        setMarker(maxId)
    }, [userId, data, keyFor])

    return { newByPeriod, markAllSeen }
}

/** New-report banner state for the signed-in user's personal reports (SSE-pushed). */
export function useNewReports(): UseNewReportsResult {
    const { data } = useReports()
    return useNewReportsCore(data, personalKey)
}

/**
 * New-report banner state for the admin system-wide reports. Only call this from an
 * admin-gated surface — useAdminReports() hits an admin-only endpoint. Admin aggregates
 * are intentionally NOT pushed over SSE, so these surface on load/navigation, not live.
 */
export function useNewAdminReports(): UseNewReportsResult {
    const { data } = useAdminReports()
    return useNewReportsCore(data, adminKey)
}
