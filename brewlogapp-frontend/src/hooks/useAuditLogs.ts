import { useState, useEffect } from 'react'
import type { AuditLog, Page } from '../types/AuditLog'
import { getAuditLogs } from '../api/adminAuditApi'

/**
 * Fetches one page of audit logs, refetching whenever `page`, `size`, or
 * `targetUserId` changes. Pass `targetUserId` to scope the trail to one account.
 */
export function useAuditLogs(page: number, size = 20, targetUserId?: number) {
    const [data, setData] = useState<Page<AuditLog> | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        getAuditLogs(page, size, targetUserId)
            .then(res => {
                if (cancelled) return
                setData(res)
                setLoading(false)
            })
            .catch(err => {
                if (cancelled) return
                setError(err)
                setLoading(false)
            })
        return () => { cancelled = true }
    }, [page, size, targetUserId])

    return { data, loading, error }
}
