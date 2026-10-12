import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAdminReports } from '../hooks/useAdminReports'
import { useNewAdminReports } from '../hooks/useNewReports'
import type { Report, ReportPeriod } from '../types/Report'
import { formatPeriodLabel, formatPeriodType } from '../types/Report'

type PeriodFilter = 'ALL' | ReportPeriod

// System-wide aggregate reports: one row per period, newest first.
function filterSortReports(reports: Report[], period: PeriodFilter): Report[] {
    const filtered = period === 'ALL' ? reports : reports.filter(r => r.periodType === period)
    return [...filtered].sort((a, b) => b.periodStart.localeCompare(a.periodStart))
}

export function AdminReportsComponent() {
    const { data, error, loading } = useAdminReports()
    const { markAllSeen } = useNewAdminReports()
    const navigate = useNavigate()
    const [period, setPeriod] = useState<PeriodFilter>('ALL')

    const visibleReports = useMemo(() => filterSortReports(data ?? [], period), [data, period])

    // Browsing the list counts as seeing them — clears the system-wide home banner.
    useEffect(() => {
        if (data && data.length) markAllSeen()
    }, [data])

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const reports = data ?? []

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">Reports — System-wide</h1>
                <p className="page-subtitle">Platform-wide brewing trends, aggregated across all users.</p>
            </header>

            {reports.length === 0 ? (
                <div className="empty">No reports yet. They're generated automatically each week and month.</div>
            ) : (
                <>
                    <div className="list-controls">
                        <div className="controls-row">
                            <label className="control">
                                <span>Period</span>
                                <select className="select" value={period} onChange={e => setPeriod(e.target.value as PeriodFilter)}>
                                    <option value="ALL">All</option>
                                    <option value="WEEKLY">Weekly</option>
                                    <option value="MONTHLY">Monthly</option>
                                </select>
                            </label>
                        </div>
                    </div>

                    {visibleReports.length === 0 ? (
                        <div className="empty">No {period.toLowerCase()} reports yet.</div>
                    ) : (
                        <>
                            <div className="list-count">{visibleReports.length} of {reports.length} reports</div>
                            <ul className="list">
                                {visibleReports.map(report => (
                                    <li
                                        key={report.id}
                                        className="list-item list-item-clickable"
                                        onClick={() => navigate(`/admin/reports/${report.id}`)}
                                    >
                                        <div className="list-item-main">
                                            <div className="list-item-title">{formatPeriodLabel(report)}</div>
                                            <div className="list-item-meta">
                                                {formatPeriodType(report.periodType)} · {report.brewCount} brew{report.brewCount === 1 ? '' : 's'}
                                                {report.activeUsers != null && ` · ${report.activeUsers} active user${report.activeUsers === 1 ? '' : 's'}`}
                                                {report.averageRating != null && ` · ${report.averageRating.toFixed(1)}★`}
                                            </div>
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        </>
                    )}
                </>
            )}
        </div>
    )
}
