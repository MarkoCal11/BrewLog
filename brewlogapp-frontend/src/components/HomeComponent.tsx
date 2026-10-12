import { Link } from 'react-router'
import { useBrewStats } from '../hooks/useBrewStats'
import { useNewReports, useNewAdminReports } from '../hooks/useNewReports'
import { useAuth } from '../context/AuthContext'
import { resolvePhotoUrl } from '../api/config'
import { formatProcessingMethod } from '../utils/beanFormat'
import { formatCategory } from '../types/FlavorNote'
import { formatPeriodLabel } from '../types/Report'
import type { Report } from '../types/Report'

function periodNoun(periodType: string): string {
    const t = (periodType ?? '').toUpperCase()
    if (t.includes('WEEK')) return 'weekly '
    if (t.includes('MONTH')) return 'monthly '
    return ''
}

function ReportBannerList({
    reports, linkTo, title, onSeen,
}: {
    reports: Report[]
    linkTo: (report: Report) => string
    title: (report: Report) => string
    onSeen: () => void
}) {
    if (reports.length === 0) return null
    return (
        <div className="stack-sm">
            {reports.map(report => (
                <div key={report.id} className="report-banner">
                    <Link to={linkTo(report)} className="report-banner-main" onClick={onSeen}>
                        <span className="report-banner-dot" aria-hidden="true" />
                        <span>
                            <strong>{title(report)}</strong>
                            {' · '}{formatPeriodLabel(report)}
                        </span>
                    </Link>
                    <button
                        type="button"
                        className="report-banner-dismiss"
                        aria-label="Dismiss"
                        onClick={onSeen}
                    >
                        ×
                    </button>
                </div>
            ))}
        </div>
    )
}


function AdminReportBanners() {
    const { newByPeriod, markAllSeen } = useNewAdminReports()
    const reports = Object.values(newByPeriod).filter((r): r is Report => r != null)
    return (
        <ReportBannerList
            reports={reports}
            linkTo={report => `/admin/reports/${report.id}`}
            title={report => `New system-wide ${periodNoun(report.periodType)}report`}
            onSeen={markAllSeen}
        />
    )
}

function greeting(): string {
    const h = new Date().getHours()
    if (h < 12) return 'Good morning'
    if (h < 18) return 'Good afternoon'
    return 'Good evening'
}

export function HomeComponent() {
    const { user } = useAuth()
    const { stats } = useBrewStats()
    const { newByPeriod, markAllSeen } = useNewReports()
    const { topBean, topMethod, recentBrews } = stats
    const topFlavors = stats.flavorStats.slice(0, 3)
    const topCategories = stats.categoryStats.slice(0, 5)


    const newReports = Object.values(newByPeriod).filter((r): r is Report => r != null)


    const subtitle = stats.totalBrews > 0
        ? `${stats.totalBrews} brews and ${stats.totalBeans} beans in your logbook so far.`
        : 'Your coffee, from first pour onward.'


    const beanPhoto = topBean?.photoUrl ? resolvePhotoUrl(topBean.photoUrl) : null
    const beanChips = (topBean?.flavorNotes ?? []).slice(0, 3)
    const beanMeta = topBean
        ? [
            `${topBean.averageRating.toFixed(1)}★ over ${topBean.brewCount} brew${topBean.brewCount === 1 ? '' : 's'}`,
            formatProcessingMethod(topBean.processingMethod)?.toLowerCase() ?? null,
            topBean.origin,
        ].filter((x): x is string => x != null).join(' · ')
        : ''

    return (
        <div className="stack-lg">
            <header className="page-header">
                {/* Editorial dateline in place of the brand kicker */}
                <div className="page-eyebrow">
                    {new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' })}
                </div>
                <h1 className="page-title">
                    {user ? `${greeting()}, ${user.firstName ?? user.username}` : greeting()}
                </h1>
                <p className="page-subtitle">{subtitle}</p>
            </header>

            <ReportBannerList
                reports={newReports}
                linkTo={report => `/reports/${report.id}`}
                title={report => `New ${periodNoun(report.periodType)}report arrived`}
                onSeen={markAllSeen}
            />

            {user?.role === 'ADMIN' && <AdminReportBanners />}

            <div className="home-actions">
                <Link to="/brews" state={{ openForm: true }} className="btn btn-primary">+ Log a brew</Link>
                <Link to="/beans" state={{ openForm: true }} className="btn btn-ghost">Add a bean</Link>
                <Link to="/cafes" className="btn btn-ghost">Find a café</Link>
            </div>

            {/* Unified stat strip: value over label (CSS turns this into one ruled bar) */}
            <div className="stat-grid">
                <div className="stat">
                    <div className="stat-value">{stats.totalBrews}</div>
                    <div className="stat-label">Brews</div>
                </div>
                <div className="stat">
                    <div className="stat-value">{stats.totalBeans}</div>
                    <div className="stat-label">Beans</div>
                </div>
                <div className="stat">
                    <div className="stat-value">{stats.avgRating != null ? `${stats.avgRating.toFixed(1)}★` : '—'}</div>
                    <div className="stat-label">Avg rating</div>
                </div>
                <div className="stat">
                    <div className="stat-value">{stats.favoriteCount}</div>
                    <div className="stat-label">Favorites</div>
                </div>
            </div>

            {/* Featured row: top-rated bean (with photo + flavor pills) beside method/flavors */}
            <div className="home-feature">
                {topBean ? (
                    <Link to={`/details/${topBean.id}`} className="hero-card hero-bean" style={{ textDecoration: 'none' }}>
                        {beanPhoto
                            ? <img className="hero-bean-photo" src={beanPhoto} alt="" />
                            : <div className="hero-bean-photo" aria-hidden="true" />}
                        <div className="hero-bean-body">
                            <div className="card-title">Top-Rated Bean</div>
                            <div className="card-strong">{topBean.name}</div>
                            <div className="card-meta">{beanMeta}</div>
                            {beanChips.length > 0 && (
                                <div className="hero-bean-chips">
                                    {beanChips.map(note => (
                                        <span key={note.id} className="chip">{note.name}</span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </Link>
                ) : (
                    <div className="hero-card hero-bean">
                        <div className="hero-bean-body">
                            <div className="card-title">Top-Rated Bean</div>
                            <div className="card-meta">No rated brews yet.</div>
                        </div>
                    </div>
                )}

                <div className="card home-side">
                    <div className="card-title">Most-Used Method</div>
                    {topMethod ? (
                        <>
                            <div className="card-strong">{topMethod.method.replace('_', ' ')}</div>
                            <div className="card-meta">
                                {topMethod.count} brew{topMethod.count === 1 ? '' : 's'} logged
                            </div>
                        </>
                    ) : (
                        <div className="card-meta">No brews logged yet.</div>
                    )}
                    <div className="card-divider" />
                    <div className="card-title">Favorite Flavors</div>
                    {topFlavors.length === 0 ? (
                        <div className="card-meta">No flavor-tagged brews yet.</div>
                    ) : (
                        <div className="chip-row" style={{ marginTop: 4 }}>
                            {topFlavors.map(stat => (
                                <span key={stat.id} className="chip">
                                    {stat.name}
                                    <span className="chip-meta">
                                        {stat.count} brew{stat.count === 1 ? '' : 's'}{stat.avgRating != null && ` · ${stat.avgRating.toFixed(1)}★`}
                                    </span>
                                </span>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            {topCategories.length > 0 && (
                <div>
                    <div className="section-title">Top Flavor Categories</div>
                    <div className="chip-row">
                        {topCategories.map(cat => (
                            <span key={cat.category} className="chip">
                                {formatCategory(cat.category)}
                                <span className="chip-meta">
                                    {cat.count} brew{cat.count === 1 ? '' : 's'}{cat.avgRating != null && ` · ${cat.avgRating.toFixed(1)}★`}
                                </span>
                            </span>
                        ))}
                    </div>
                </div>
            )}

            <div>
                <div className="section-title">Recent Brews</div>
                {recentBrews.length === 0 ? (
                    <div className="empty">No brews logged yet.</div>
                ) : (
                    <ul className="list">
                        {recentBrews.map(brew => (
                            <li key={brew.id} className="list-item list-item-clickable">
                                <Link to={`/brews/${brew.id}`} className="list-item-main">
                                    <div className="list-item-title">{brew.coffeeBean?.name}</div>
                                    <div className="list-item-meta">
                                        {brew.method.replace('_', ' ')} · {new Date(brew.brewedAt).toLocaleDateString()}
                                    </div>
                                </Link>
                                {brew.rating != null && <div className="rating">{brew.rating}★</div>}
                            </li>
                        ))}
                    </ul>
                )}
            </div>

            <div style={{ textAlign: 'center' }}>
                <Link to="/export" className="btn btn-ghost">Export your data →</Link>
            </div>
        </div>
    )
}
