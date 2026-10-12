import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useReports } from '../hooks/useReports'
import { useNewReports } from '../hooks/useNewReports'
import type { Report, ReportPeriod } from '../types/Report'
import { formatPeriodLabel, formatPeriodType } from '../types/Report'

type PeriodFilter = 'ALL' | ReportPeriod

// Newest period first; reports never overlap within a period type, so periodStart sorts cleanly.
function sortReports(reports: Report[], period: PeriodFilter): Report[] {
    const filtered = period === 'ALL' ? reports : reports.filter(r => r.periodType === period)
    return [...filtered].sort((a, b) => b.periodStart.localeCompare(a.periodStart))
}

export function ReportListComponent() {
    const { data, error, loading } = useReports()
    const { markAllSeen } = useNewReports()
    const navigate = useNavigate()
    const [period, setPeriod] = useState<PeriodFilter>('ALL')

    const visibleReports = useMemo(() => sortReports(data ?? [], period), [data, period])

    // Browsing the list counts as seeing them — clears the "new report" home banner.
    useEffect(() => {
        if (data && data.length) markAllSeen()
    }, [data])

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const reports = data ?? []

    return (
        <div className="stack-lg">
            <header className="page-header">
                <div className="page-eyebrow">BrewLog</div>
                <h1 className="page-title">Reports</h1>
                <p className="page-subtitle">Your brewing, summarized each week and month</p>
            </header>

            {reports.length === 0 ? (
                <div className="empty">No reports yet. They're generated automatically as you log brews.</div>
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
                                        onClick={() => navigate(`/reports/${report.id}`)}
                                    >
                                        <div className="list-item-main">
                                            <div className="list-item-title">{formatPeriodLabel(report)}</div>
                                            <div className="list-item-meta">
                                                {formatPeriodType(report.periodType)} · {report.brewCount} brew{report.brewCount === 1 ? '' : 's'}
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
