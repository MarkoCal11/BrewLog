import { useState, useEffect, useCallback } from 'react'
import type { Report } from '../types/Report'
import { getAllReports } from '../api/reportApi'
import { subscribeReportEvents } from '../api/reportStream'

type UseReportsResult = {
    data: Report[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useReports(): UseReportsResult {
    const [data, setData] = useState<Report[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    // No synchronous setLoading(true) here: `loading` starts true for the initial
    // load, and on a refetch (manual or SSE-triggered) we keep the current data on
    // screen rather than flashing a spinner. State is only set once the fetch settles.
    const fetchReports = useCallback(() => {
        getAllReports()
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

    // Live-update: when the backend pushes a "new report" event over SSE, refetch so
    // the home banner / reports list reflect it without a page reload. The connection
    // is a shared singleton, so every mounted consumer subscribing here is cheap.
    useEffect(() => subscribeReportEvents(fetchReports), [fetchReports])

    return { data, loading, error, refetch: fetchReports }
}
