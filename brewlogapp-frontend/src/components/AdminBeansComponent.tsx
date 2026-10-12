import { useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAdminCoffeeBeans } from '../hooks/useAdminCoffeeBeans'
import { formatFreshness, formatPrice, formatPricePer100g, formatProcessingMethod } from '../utils/beanFormat'
import { filterSortBeans } from '../utils/listFilters'
import type { BeanSort } from '../utils/listFilters'
import { PROCESSING_METHODS, ROAST_LEVELS, formatRoastLevel } from '../types/CoffeeBean'
import type { FlavorNote } from '../types/FlavorNote'
import { ListToolbar } from './ListToolbar'

export function AdminBeansComponent() {
    const { data, error, loading } = useAdminCoffeeBeans()
    const navigate = useNavigate()

    const [search, setSearch] = useState('')
    const [roastFilter, setRoastFilter] = useState('')
    const [methodFilter, setMethodFilter] = useState('')
    const [noteFilter, setNoteFilter] = useState('')
    const [ownerFilter, setOwnerFilter] = useState('')
    const [sort, setSort] = useState<BeanSort>('name')

    // Filter options derived from the data, since admin beans span all users.
    const owners = useMemo(
        () => Array.from(new Set((data ?? []).map(b => b.ownerUsername).filter(Boolean) as string[])).sort(),
        [data],
    )
    const notes = useMemo(() => {
        const map = new Map<number, FlavorNote>()
        for (const bean of data ?? []) {
            for (const n of bean.flavorNotes ?? []) map.set(n.id, n)
        }
        return Array.from(map.values()).sort((a, b) => a.name.localeCompare(b.name))
    }, [data])

    const visibleBeans = useMemo(
        () => filterSortBeans(data ?? [], {
            search, roast: roastFilter, method: methodFilter, note: noteFilter, fav: false, owner: ownerFilter, sort,
        }),
        [data, search, roastFilter, methodFilter, noteFilter, ownerFilter, sort],
    )

    const hasFilters = search !== '' || roastFilter !== '' || methodFilter !== '' || noteFilter !== '' || ownerFilter !== '' || sort !== 'name'
    const clearFilters = () => {
        setSearch('')
        setRoastFilter('')
        setMethodFilter('')
        setNoteFilter('')
        setOwnerFilter('')
        setSort('name')
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">All Beans</h1>
                <p className="page-subtitle">{(data ?? []).length} beans across all users.</p>
            </header>

            {(data ?? []).length === 0 ? (
                <div className="empty">No beans found.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search name, origin, roaster, flavor, owner…"
                        showClear={hasFilters}
                        onClear={clearFilters}
                    >
                        <label className="control">
                            <span>Sort</span>
                            <select className="select" value={sort} onChange={e => setSort(e.target.value as BeanSort)}>
                                <option value="name">Name A–Z</option>
                                <option value="rating">Top rated</option>
                                <option value="brews">Most brewed</option>
                                <option value="fresh">Freshest</option>
                                <option value="price">Cheapest /100g</option>
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
                            <span>Roast</span>
                            <select className="select" value={roastFilter} onChange={e => setRoastFilter(e.target.value)}>
                                <option value="">All</option>
                                {ROAST_LEVELS.map(r => (
                                    <option key={r} value={r}>{formatRoastLevel(r)}</option>
                                ))}
                            </select>
                        </label>
                        <label className="control">
                            <span>Process</span>
                            <select className="select" value={methodFilter} onChange={e => setMethodFilter(e.target.value)}>
                                <option value="">All</option>
                                {PROCESSING_METHODS.map(pm => (
                                    <option key={pm} value={pm}>{formatProcessingMethod(pm)}</option>
                                ))}
                            </select>
                        </label>
                        {notes.length > 0 && (
                            <label className="control">
                                <span>Flavor</span>
                                <select className="select" value={noteFilter} onChange={e => setNoteFilter(e.target.value)}>
                                    <option value="">All</option>
                                    {notes.map(n => <option key={n.id} value={n.id}>{n.name}</option>)}
                                </select>
                            </label>
                        )}
                    </ListToolbar>

                    {visibleBeans.length === 0 ? (
                        <div className="empty">No beans match your filters.</div>
                    ) : (
                        <>
                            <div className="list-count">{visibleBeans.length} of {(data ?? []).length} beans</div>
                            <ul className="list">
                                {visibleBeans.map(bean => {
                                    const freshness = formatFreshness(bean.roastDate)
                                    const price = formatPricePer100g(bean.price, bean.weightGrams) ?? formatPrice(bean.price)
                                    return (
                                        <li
                                            key={bean.id}
                                            className="list-item list-item-clickable"
                                            onClick={() => navigate(`/admin/beans/${bean.id}`)}
                                        >
                                            <div className="list-item-main">
                                                <div className="list-item-title">{bean.name}</div>
                                                <div className="list-item-meta">
                                                    {bean.origin} · {formatRoastLevel(bean.roastLevel).toLowerCase()}
                                                    {bean.brewCount > 0 && ` · ${bean.averageRating.toFixed(1)}★`}
                                                    {freshness && ` · ${freshness}`}
                                                    {price && ` · ${price}`}
                                                </div>
                                            </div>
                                            {bean.ownerUsername && <span className="owner-badge">{bean.ownerUsername}</span>}
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
