import { useState, useEffect } from 'react'
import { useParams } from 'react-router'
import { getReport } from '../api/reportApi'
import type { Report } from '../types/Report'
import { ReportDetailView } from './ReportDetailView'

export function ReportDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const [report, setReport] = useState<Report | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    useEffect(() => {
        getReport(Number(id))
            .then(setReport)
            .catch(err => setError(err))
            .finally(() => setLoading(false))
    }, [id])

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!report) return <div className="empty">Report not found.</div>

    return <ReportDetailView report={report} backTo={{ to: '/reports', label: 'Reports' }} />
}
