import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAdminCafeLocations } from '../hooks/useAdminCafeLocations'
import type { CafeLocation } from '../types/CafeLocation'
import { hasCoords } from '../types/CafeLocation'
import { ListToolbar } from './ListToolbar'
import { CafeMap } from './CafeMap'
import { StarDisplay } from './StarPicker'

type CafeSort = 'name' | 'rating' | 'city'

function filterSortCafes(cafes: CafeLocation[], search: string, owner: string, sort: CafeSort): CafeLocation[] {
    const q = search.trim().toLowerCase()
    const filtered = cafes.filter(c => {
        if (owner && c.ownerUsername !== owner) return false
        if (!q) return true
        return (
            c.name.toLowerCase().includes(q) ||
            (c.city ?? '').toLowerCase().includes(q) ||
            (c.address ?? '').toLowerCase().includes(q) ||
            (c.ownerUsername ?? '').toLowerCase().includes(q)
        )
    })
    const sorted = [...filtered]
    sorted.sort((a, b) => {
        if (sort === 'rating') return (b.rating ?? -1) - (a.rating ?? -1)
        if (sort === 'city') return (a.city ?? '').localeCompare(b.city ?? '')
        return a.name.localeCompare(b.name)
    })
    return sorted
}

export function AdminCafesComponent() {
    const { data, error, loading } = useAdminCafeLocations()
    const navigate = useNavigate()

    const [search, setSearch] = useState('')
    const [ownerFilter, setOwnerFilter] = useState('')
    const [sort, setSort] = useState<CafeSort>('name')

    const owners = useMemo(
        () => Array.from(new Set((data ?? []).map(c => c.ownerUsername).filter(Boolean) as string[])).sort(),
        [data],
    )

    const visibleCafes = useMemo(
        () => filterSortCafes(data ?? [], search, ownerFilter, sort),
        [data, search, ownerFilter, sort],
    )

    const hasFilters = search !== '' || ownerFilter !== '' || sort !== 'name'
    const clearFilters = () => {
        setSearch('')
        setOwnerFilter('')
        setSort('name')
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const cafes = data ?? []
    const anyLocated = cafes.some(hasCoords)

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">All Cafés</h1>
                <p className="page-subtitle">{cafes.length} cafés across all users.</p>
            </header>

            {cafes.length === 0 ? (
                <div className="empty">No cafés found.</div>
            ) : (
                <>
                    {anyLocated && <CafeMap cafes={cafes} detailBase="/admin/cafes" />}

                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search name, city, address, owner…"
                        showClear={hasFilters}
                        onClear={clearFilters}
                    >
                        <label className="control">
                            <span>Sort</span>
                            <select className="select" value={sort} onChange={e => setSort(e.target.value as CafeSort)}>
                                <option value="name">Name A–Z</option>
                                <option value="rating">Top rated</option>
                                <option value="city">City A–Z</option>
                            </select>
                        </label>
                        <label className="control">
                            <span>Owner</span>
                            <select className="select" value={ownerFilter} onChange={e => setOwnerFilter(e.target.value)}>
                                <option value="">All</option>
                                {owners.map(o => <option key={o} value={o}>{o}</option>)}
                            </select>
                        </label>
                    </ListToolbar>

                    {visibleCafes.length === 0 ? (
                        <div className="empty">No cafés match your filters.</div>
                    ) : (
                        <>
                            <div className="list-count">{visibleCafes.length} of {cafes.length} cafés</div>
                            <ul className="list">
                                {visibleCafes.map(cafe => (
                                    <li
                                        key={cafe.id}
                                        className="list-item list-item-clickable"
                                        onClick={() => navigate(`/admin/cafes/${cafe.id}`)}
                                    >
                                        <div className="list-item-main">
                                            <div className="list-item-title">{cafe.name}</div>
                                            <div className="list-item-meta">
                                                {cafe.city ?? 'Unknown city'}
                                                {cafe.rating != null && <> · <StarDisplay value={cafe.rating} /></>}
                                            </div>
                                        </div>
                                        {cafe.ownerUsername && <span className="owner-badge">{cafe.ownerUsername}</span>}
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
