import { Link } from 'react-router'
import {
    ResponsiveContainer,
    LineChart,
    Line,
    BarChart,
    Bar,
    XAxis,
    YAxis,
    Tooltip,
    CartesianGrid,
} from 'recharts'
import type { Report } from '../types/Report'
import { formatPeriodLabel, formatPeriodType, formatGeneratedAt, shortDay } from '../types/Report'
import { formatMethod } from '../types/BrewEntry'
import type { Method } from '../types/BrewEntry'

type ReportDetailViewProps = {
    report: Report
    backTo: { to: string; label: string }
}

const fmtRating = (r: number | null) => (r == null ? '—' : `${r.toFixed(1)}★`)

const axisTick = { fill: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }
const tooltipStyle = {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 10,
    color: 'var(--text)',
}

export function ReportDetailView({ report, backTo }: ReportDetailViewProps) {
    const brewsPerDay = report.brewsPerDay.map(d => ({ label: shortDay(d.date), count: d.count }))
    const ratingTrend = report.ratingTrend.map(d => ({ label: shortDay(d.date), rating: d.avgRating }))
    const methodBars = report.methodBreakdown.map(m => ({
        method: formatMethod(m.method as Method),
        count: m.count,
    }))
    const hasRatingTrend = ratingTrend.filter(d => d.rating != null).length >= 2

    return (
        <div className="stack-lg">
            <Link to={backTo.to} className="back-link">{backTo.label}</Link>

            <header className="page-header">
                <h1 className="page-title">{formatPeriodLabel(report)}</h1>
                <p className="page-subtitle">
                    {report.activeUsers != null && 'System-wide · '}
                    {formatPeriodType(report.periodType)} report
                    {' · '}generated {formatGeneratedAt(report.generatedAt)}
                </p>
            </header>

            {/* --- headline numbers --- */}
            <div className="stat-grid">
                <div className="stat-card">
                    <div className="stat-value">{report.brewCount}</div>
                    <div className="stat-label">Brews logged</div>
                </div>
                {report.activeUsers != null && (
                    <div className="stat-card">
                        <div className="stat-value">{report.activeUsers}</div>
                        <div className="stat-label">Active users</div>
                    </div>
                )}
                <div className="stat-card">
                    <div className="stat-value">{fmtRating(report.averageRating)}</div>
                    <div className="stat-label">Avg rating</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{report.mostUsedMethod ? formatMethod(report.mostUsedMethod as Method) : '—'}</div>
                    <div className="stat-label">Most-used method</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{report.totalDoseGrams == null ? '—' : `${Math.round(report.totalDoseGrams)}g`}</div>
                    <div className="stat-label">Total dose</div>
                </div>
            </div>

            {report.topBeanName && (
                <section className="card">
                    <div className="card-title">Top bean</div>
                    <div className="card-strong">{report.topBeanName}</div>
                </section>
            )}

            {/* --- brews per day --- */}
            <section className="card">
                <div className="card-title">Brews per day</div>
                {brewsPerDay.length > 0 ? (
                    <div className="chart-box">
                        <ResponsiveContainer width="100%" height={240}>
                            <BarChart data={brewsPerDay} margin={{ top: 8, right: 12, bottom: 0, left: -16 }}>
                                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="label" tick={axisTick} stroke="var(--border)" interval={0} />
                                <YAxis allowDecimals={false} tick={axisTick} stroke="var(--border)" />
                                <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'var(--accent-soft)' }} />
                                <Bar dataKey="count" fill="var(--accent)" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                ) : (
                    <div className="card-meta">No brews logged in this period.</div>
                )}
            </section>

            {/* --- rating over time --- */}
            <section className="card">
                <div className="card-title">Rating over time</div>
                {hasRatingTrend ? (
                    <div className="chart-box">
                        <ResponsiveContainer width="100%" height={240}>
                            <LineChart data={ratingTrend} margin={{ top: 8, right: 12, bottom: 0, left: -16 }}>
                                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" />
                                <XAxis dataKey="label" tick={axisTick} stroke="var(--border)" />
                                <YAxis domain={[0, 5]} tick={axisTick} stroke="var(--border)" />
                                <Tooltip contentStyle={tooltipStyle} formatter={(v: number) => [`${v.toFixed(1)}★`, 'Avg rating']} />
                                <Line
                                    type="monotone"
                                    dataKey="rating"
                                    stroke="var(--accent)"
                                    strokeWidth={2}
                                    dot={{ r: 3, fill: 'var(--accent)' }}
                                    connectNulls
                                />
                            </LineChart>
                        </ResponsiveContainer>
                    </div>
                ) : (
                    <div className="card-meta">Need rated brews on at least two days to chart a trend.</div>
                )}
            </section>

            {/* --- brews by method --- */}
            <section className="card">
                <div className="card-title">Brews by method</div>
                {methodBars.length > 0 ? (
                    <div className="chart-box">
                        <ResponsiveContainer width="100%" height={240}>
                            <BarChart data={methodBars} margin={{ top: 8, right: 12, bottom: 0, left: -16 }}>
                                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="method" tick={axisTick} stroke="var(--border)" interval={0} />
                                <YAxis allowDecimals={false} tick={axisTick} stroke="var(--border)" />
                                <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'var(--accent-soft)' }} />
                                <Bar dataKey="count" fill="var(--accent)" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                ) : (
                    <div className="card-meta">No brews logged in this period.</div>
                )}
            </section>
        </div>
    )
}
