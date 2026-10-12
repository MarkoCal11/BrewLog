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
import { formatPrice, formatPer100gValue, formatProcessingMethod } from '../utils/beanFormat'
import { formatCategory } from '../types/FlavorNote'
import { formatRoastLevel } from '../types/CoffeeBean'
import type { BrewStats } from '../utils/brewStats'

type StatsViewProps = {
    stats: BrewStats
    loading: boolean
    error: Error | null
    title: string
    subtitle: string
    /** When set, renders a back-link (used by the admin global view). */
    backTo?: { to: string; label: string }
}

const fmtRating = (r: number | null) => (r == null ? '—' : `${r.toFixed(1)}★`)
const fmtRatio = (r: number | null) => (r == null ? '—' : `1:${r.toFixed(1)}`)
const fmtNum = (n: number | null, suffix = '') => (n == null ? '—' : `${Math.round(n)}${suffix}`)
const fmtDuration = (seconds: number | null) => {
    if (seconds == null) return '—'
    const total = Math.round(seconds)
    return `${Math.floor(total / 60)}:${String(total % 60).padStart(2, '0')}`
}

const axisTick = { fill: 'var(--text-secondary)', fontSize: 13, fontWeight: 500 }
const tooltipStyle = {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderRadius: 10,
    color: 'var(--text)',
}

export function StatsView({ stats, loading, error, title, subtitle, backTo }: StatsViewProps) {
    if (loading) return <div className="empty">Loading analytics…</div>
    if (error) return <div className="empty">Couldn’t load analytics.</div>

    const header = (
        <header className="page-header">
            {backTo && <Link to={backTo.to} className="back-link">{backTo.label}</Link>}
            {/* Eyebrow only on the top-level tab; drill-downs use the back-link instead. */}
            {!backTo && <div className="page-eyebrow">BrewLog</div>}
            <h1 className="page-title">{title}</h1>
            <p className="page-subtitle">{subtitle}</p>
        </header>
    )

    if (stats.totalBrews === 0 && stats.totalBeans === 0) {
        return (
            <div className="stack-lg">
                {header}
                <div className="empty">No beans or brews to chart yet.</div>
            </div>
        )
    }

    const ratingTrend = stats.byMonth.map(m => ({ label: m.label, rating: m.avgRating }))
    const methodBars = stats.byMethod.map(m => ({ method: m.method.replace('_', ' '), count: m.count }))
    const hasRatingTrend = ratingTrend.filter(m => m.rating != null).length >= 2
    const categoryBars = stats.categoryStats.map(c => ({ category: formatCategory(c.category), count: c.count }))

    return (
        <div className="stack-lg">
            {header}

            {/* --- headline numbers --- */}
            <div className="stat-grid">
                <div className="stat-card">
                    <div className="stat-value">{stats.totalBrews}</div>
                    <div className="stat-label">Brews logged</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{fmtRating(stats.avgRating)}</div>
                    <div className="stat-label">Avg rating ({stats.ratedCount} rated)</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.totalBeans}</div>
                    <div className="stat-label">Beans · <span style={{ whiteSpace: 'nowrap' }}>{stats.favoriteCount} favorites</span></div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.untriedCount}</div>
                    <div className="stat-label">Untried beans</div>
                </div>
            </div>

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
                    <div className="card-meta">Need rated brews across at least two months to chart a trend.</div>
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
                    <div className="card-meta">No brews logged yet.</div>
                )}
            </section>

            {/* --- per-method breakdown --- */}
            {stats.byMethod.length > 0 && (
                <section>
                    <div className="section-title">By method</div>
                    <div className="table-wrap">
                        <table className="stat-table">
                            <thead>
                                <tr>
                                    <th>Method</th>
                                    <th>Brews</th>
                                    <th>Avg ★</th>
                                    <th>Grind</th>
                                    <th>Temp</th>
                                    <th>Time</th>
                                </tr>
                            </thead>
                            <tbody>
                                {stats.byMethod.map(m => (
                                    <tr key={m.method}>
                                        <td>{m.method.replace('_', ' ')}</td>
                                        <td>{m.count}</td>
                                        <td>{fmtRating(m.avgRating)}</td>
                                        <td>{m.avgGrind == null ? '—' : m.avgGrind.toFixed(1)}</td>
                                        <td>{fmtNum(m.avgTempC, '°C')}</td>
                                        <td>{fmtDuration(m.avgTimeSeconds)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            {/* --- by processing method --- */}
            {stats.byProcessingMethod.length > 0 && (
                <section>
                    <div className="section-title">By processing method</div>
                    <div className="table-wrap">
                        <table className="stat-table">
                            <thead>
                                <tr>
                                    <th>Processing</th>
                                    <th>Brews</th>
                                    <th>Avg ★</th>
                                </tr>
                            </thead>
                            <tbody>
                                {stats.byProcessingMethod.map(p => (
                                    <tr key={p.method}>
                                        <td>{formatProcessingMethod(p.method)}</td>
                                        <td>{p.count}</td>
                                        <td>{fmtRating(p.avgRating)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            )}

            {/* --- flavor categories (only present on the personal view) --- */}
            {categoryBars.length > 0 && (
                <section className="card">
                    <div className="card-title">Flavor categories</div>
                    <div className="chart-box">
                        <ResponsiveContainer width="100%" height={240}>
                            <BarChart data={categoryBars} margin={{ top: 8, right: 12, bottom: 0, left: -16 }}>
                                <CartesianGrid stroke="var(--border)" strokeDasharray="3 3" vertical={false} />
                                <XAxis dataKey="category" tick={axisTick} stroke="var(--border)" interval={0} />
                                <YAxis allowDecimals={false} tick={axisTick} stroke="var(--border)" />
                                <Tooltip contentStyle={tooltipStyle} cursor={{ fill: 'var(--accent-soft)' }} />
                                <Bar dataKey="count" fill="var(--accent)" radius={[6, 6, 0, 0]} />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </section>
            )}

            {/* --- top flavors --- */}
            {stats.flavorStats.length > 0 && (
                <section className="card">
                    <div className="card-title">Top flavors</div>
                    <div className="chip-row" style={{ marginTop: 4 }}>
                        {stats.flavorStats.slice(0, 12).map(f => (
                            <span key={f.id} className="chip">
                                {f.name}
                                <span className="chip-meta">
                                    {f.count} brew{f.count === 1 ? '' : 's'}{f.avgRating != null && ` · ${f.avgRating.toFixed(1)}★`}
                                </span>
                            </span>
                        ))}
                    </div>
                </section>
            )}

            {/* --- recipe ratios --- */}
            <section className="card">
                <div className="card-title">Recipe ratios</div>
                <div className="ratio-row">
                    <div>
                        <div className="card-strong">{fmtRatio(stats.ratios.avgBrewRatio)}</div>
                        <div className="card-meta">
                            Avg brew ratio · {stats.ratios.brewRatioCoverage} of {stats.totalBrews} brews
                        </div>
                    </div>
                    <div>
                        <div className="card-strong">{fmtRatio(stats.ratios.avgYieldRatio)}</div>
                        <div className="card-meta">
                            Avg yield ratio · {stats.ratios.yieldRatioCoverage} of {stats.totalBrews} brews
                        </div>
                    </div>
                </div>
            </section>

            {/* --- cost per brew --- */}
            <section className="card">
                <div className="card-title">Cost per brew</div>
                {stats.costCoverage > 0 ? (
                    <div className="ratio-row">
                        <div>
                            <div className="card-strong">{formatPrice(stats.avgCostPerBrew)}</div>
                            <div className="card-meta">
                                Avg per brew · {stats.costCoverage} of {stats.totalBrews} brews
                            </div>
                        </div>
                        <div>
                            <div className="card-strong">{formatPrice(stats.totalBrewCost)}</div>
                            <div className="card-meta">Total over those {stats.costCoverage} brews</div>
                        </div>
                    </div>
                ) : (
                    <div className="card-meta">Add bag price + weight to your beans to see brew costs.</div>
                )}
            </section>

            {/* --- freshness at brew --- */}
            <section className="card">
                <div className="card-title">Freshness at brew</div>
                {stats.daysOffRoastCoverage > 0 ? (
                    <>
                        <div className="card-strong">
                            {stats.avgDaysOffRoast == null ? '—' : `${Math.round(stats.avgDaysOffRoast)} days off roast`}
                        </div>
                        <div className="card-meta">
                            Avg · {stats.daysOffRoastCoverage} of {stats.totalBrews} brews
                        </div>
                    </>
                ) : (
                    <div className="card-meta">Add roast dates to your beans to see how fresh you brew.</div>
                )}
            </section>

            {/* --- beans --- */}
            <section>
                <div className="section-title">Beans</div>
                <div className="card">
                    <div className="card-title">Roast levels</div>
                    <div className="chip-row" style={{ marginTop: 4 }}>
                        {stats.roastLevels.map(r => (
                            <span key={r.level} className="chip">
                                {formatRoastLevel(r.level)}
                                <span className="chip-meta">{r.count}</span>
                            </span>
                        ))}
                    </div>
                </div>

                <div className="stat-grid" style={{ marginTop: 12 }}>
                    <div className="stat-card">
                        <div className="stat-value">{formatPer100gValue(stats.avgPricePer100g) ?? '—'}</div>
                        <div className="stat-label">Avg price / 100g</div>
                    </div>
                </div>

                {stats.topOrigins.length > 0 && (
                    <div className="card" style={{ marginTop: 12 }}>
                        <div className="card-title">Top origins</div>
                        <div className="chip-row" style={{ marginTop: 4 }}>
                            {stats.topOrigins.map(o => (
                                <span key={o.label} className="chip">
                                    {o.label}
                                    <span className="chip-meta">{o.count}</span>
                                </span>
                            ))}
                        </div>
                    </div>
                )}

                {stats.topRoasters.length > 0 && (
                    <div className="card" style={{ marginTop: 12 }}>
                        <div className="card-title">Top roasters</div>
                        <div className="chip-row" style={{ marginTop: 4 }}>
                            {stats.topRoasters.map(r => (
                                <span key={r.label} className="chip">
                                    {r.label}
                                    <span className="chip-meta">{r.count}</span>
                                </span>
                            ))}
                        </div>
                    </div>
                )}
            </section>
        </div>
    )
}
