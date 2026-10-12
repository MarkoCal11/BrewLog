import { useCallback, useEffect, useState } from 'react'
import type { Report } from '../types/Report'
import { getAllReportsGlobal } from '../api/reportApi'

type UseAdminReportsResult = {
    data: Report[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useAdminReports(): UseAdminReportsResult {
    const [data, setData] = useState<Report[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    // `quiet` skips the loading flag so a background refetch keeps the current list on
    // screen instead of flashing the loading state (only the initial load shows it).
    const fetchReports = useCallback((quiet = false) => {
        if (!quiet) setLoading(true)
        getAllReportsGlobal()
            .then(reports => {
                setData(reports)
                setError(null)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }, [])

    useEffect(() => { fetchReports() }, [fetchReports])

    // Admin aggregate reports are deliberately not SSE-pushed (they're generated weekly/
    // monthly, usually off-hours when no admin is connected). Instead, refetch when the
    // admin returns to the tab — on window focus or the tab becoming visible — so a report
    // generated while they were away shows up without a manual reload. Quiet refetch, and
    // never while hidden. Scoped to this hook, so it doesn't touch the per-user SSE flow.
    useEffect(() => {
        const refetchIfVisible = () => {
            if (document.visibilityState === 'visible') fetchReports(true)
        }
        window.addEventListener('focus', refetchIfVisible)
        document.addEventListener('visibilitychange', refetchIfVisible)
        return () => {
            window.removeEventListener('focus', refetchIfVisible)
            document.removeEventListener('visibilitychange', refetchIfVisible)
        }
    }, [fetchReports])

    return { data, loading, error, refetch: fetchReports }
}
