import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAdminBrewEntries } from '../hooks/useAdminBrewEntries'
import type { BrewEntry } from '../types/BrewEntry'
import { METHODS, formatMethod } from '../types/BrewEntry'
import { filterSortBrews } from '../utils/listFilters'
import type { BrewSort } from '../utils/listFilters'
import { ListToolbar } from './ListToolbar'

const formatTime = (seconds: number | null) => {
    if (seconds == null) return null
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${String(s).padStart(2, '0')}`
}

const formatRatio = (entry: BrewEntry) => {
    if (!entry.doseGrams) return null
    if (entry.method === 'ESPRESSO' && entry.yieldGrams) {
        return `1:${(entry.yieldGrams / entry.doseGrams).toFixed(1)}`
    }
    if (entry.waterGrams) {
        return `1:${(entry.waterGrams / entry.doseGrams).toFixed(1)}`
    }
    return null
}

export function AdminBrewsComponent() {
    const { data, error, loading } = useAdminBrewEntries()
    const navigate = useNavigate()

    const [search, setSearch] = useState('')
    const [methodFilter, setMethodFilter] = useState('')
    const [beanFilter, setBeanFilter] = useState('')
    const [ownerFilter, setOwnerFilter] = useState('')
    const [sort, setSort] = useState<BrewSort>('recent')

    // Filter options derived from the data, since admin brews span all users.
    const owners = useMemo(
        () => Array.from(new Set((data ?? []).map(e => e.ownerUsername).filter(Boolean) as string[])).sort(),
        [data],
    )
    const brewBeans = useMemo(
        () => Array.from(new Map((data ?? []).map(e => [e.coffeeBean.id, e.coffeeBean])).values())
            .sort((a, b) => a.name.localeCompare(b.name)),
        [data],
    )

    const visibleBrews = useMemo(
        () => filterSortBrews(data ?? [], { search, method: methodFilter, bean: beanFilter, owner: ownerFilter, sort }),
        [data, search, methodFilter, beanFilter, ownerFilter, sort],
    )

    const hasFilters = search !== '' || methodFilter !== '' || beanFilter !== '' || ownerFilter !== '' || sort !== 'recent'
    const clearFilters = () => {
        setSearch('')
        setMethodFilter('')
        setBeanFilter('')
        setOwnerFilter('')
        setSort('recent')
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">All Brews</h1>
                <p className="page-subtitle">{(data ?? []).length} brews across all users.</p>
            </header>

            {(data ?? []).length === 0 ? (
                <div className="empty">No brews found.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search bean, method, notes, owner…"
                        showClear={hasFilters}
                        onClear={clearFilters}
                    >
                        <label className="control">
                            <span>Sort</span>
                            <select className="select" value={sort} onChange={e => setSort(e.target.value as BrewSort)}>
                                <option value="recent">Newest first</option>
                                <option value="oldest">Oldest first</option>
                                <option value="rating">Highest rated</option>
                            </select>
                        </label>
                        <label className="control">
                            <span>Owner</span>
                            <select className="select" value={ownerFilter} onChange={e => setOwnerFilter(e.target.value)}>
                                <option value="">All</option>
                                {owners.map(o => <option key={o} value={o}>{o}</option>)}
                            </select>
                        </label>
                        <label className="control">
                            <span>Method</span>
                            <select className="select" value={methodFilter} onChange={e => setMethodFilter(e.target.value)}>
                                <option value="">All</option>
                                {METHODS.map(m => <option key={m} value={m}>{formatMethod(m)}</option>)}
                            </select>
                        </label>
                        <label className="control">
                            <span>Bean</span>
                            <select className="select" value={beanFilter} onChange={e => setBeanFilter(e.target.value)}>
                                <option value="">All</option>
                                {brewBeans.map(b => <option key={b.id} value={b.id}>{b.name}</option>)}
                            </select>
                        </label>
                    </ListToolbar>

                    {visibleBrews.length === 0 ? (
                        <div className="empty">No brews match your filters.</div>
                    ) : (
                        <>
                            <div className="list-count">{visibleBrews.length} of {(data ?? []).length} brews</div>
                            <ul className="list">
                                {visibleBrews.map(entry => {
                                    const ratio = formatRatio(entry)
                                    const time = formatTime(entry.brewTimeSeconds)
                                    return (
                                        <li
                                            key={entry.id}
                                            className="list-item list-item-clickable"
                                            onClick={() => navigate(`/admin/brews/${entry.id}`)}
                                        >
                                            <div className="list-item-main">
                                                <div className="list-item-title">{entry.coffeeBean?.name}</div>
                                                <div className="list-item-meta">
                                                    {formatMethod(entry.method)}
                                                    {time && ` · ${time} brew time`}
                                                    {ratio && ` · ${ratio} ratio`}
                                                    {` · ${new Date(entry.brewedAt).toLocaleDateString()}`}
                                                </div>
                                            </div>
                                            {entry.ownerUsername && <span className="owner-badge">{entry.ownerUsername}</span>}
                                            {entry.rating != null && <div className="rating">{entry.rating}★</div>}
                                        </li>
                                    )
                                })}
                            </ul>
                        </>
                    )}
                </>
            )}
        </div>
    )
}
