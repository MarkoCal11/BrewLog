import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router'
import { useCafeLocations } from '../hooks/useCafeLocations'
import { createCafeLocation, deleteCafeLocation } from '../api/cafeLocationApi'
import type { CafeLocationCreateRequest } from '../api/cafeLocationApi'
import type { CafeLocation } from '../types/CafeLocation'
import { hasCoords } from '../types/CafeLocation'
import { extractErrorMessage } from '../utils/errors'
import { ListToolbar } from './ListToolbar'
import { CafeLocationForm } from './CafeLocationForm'
import { CafeMap } from './CafeMap'

type CafeSort = 'name' | 'rating' | 'city'

function filterSortCafes(cafes: CafeLocation[], search: string, sort: CafeSort): CafeLocation[] {
    const q = search.trim().toLowerCase()
    const filtered = q
        ? cafes.filter(c =>
            c.name.toLowerCase().includes(q) ||
            (c.city ?? '').toLowerCase().includes(q) ||
            (c.address ?? '').toLowerCase().includes(q))
        : cafes
    const sorted = [...filtered]
    sorted.sort((a, b) => {
        if (sort === 'rating') return (b.rating ?? -1) - (a.rating ?? -1)
        if (sort === 'city') return (a.city ?? '').localeCompare(b.city ?? '')
        return a.name.localeCompare(b.name)
    })
    return sorted
}

export function CafeLocationListComponent() {
    const { data, error, loading, refetch, removeCafe } = useCafeLocations()
    const navigate = useNavigate()

    const [showForm, setShowForm] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    const [search, setSearch] = useState('')
    const [sort, setSort] = useState<CafeSort>('name')

    const visibleCafes = useMemo(
        () => filterSortCafes(data ?? [], search, sort),
        [data, search, sort],
    )
    const hasFilters = search !== '' || sort !== 'name'
    const clearFilters = () => {
        setSearch('')
        setSort('name')
    }

    const handleCreate = async (payload: CafeLocationCreateRequest) => {
        setSaving(true)
        setSaveError(null)
        try {
            await createCafeLocation(payload)
            setShowForm(false)
            refetch()
        } catch (err) {
            setSaveError(extractErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (e: React.MouseEvent, cafe: CafeLocation) => {
        e.stopPropagation()
        if (!window.confirm(`Delete "${cafe.name}"? This cannot be undone.`)) return
        setDeleteError(null)
        try {
            removeCafe(cafe.id)
            await deleteCafeLocation(cafe.id)
        } catch (err) {
            console.error('Failed to delete café:', err)
            setDeleteError(extractErrorMessage(err))
            refetch()
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const cafes = data ?? []
    const anyLocated = cafes.some(hasCoords)

    return (
        <div className="stack-lg">
            <header className="row-between">
                <div>
                    <div className="page-eyebrow">BrewLog</div>
                    <h1 className="page-title">Cafés</h1>
                    <p className="page-subtitle">Cafés worth remembering</p>
                </div>
                {!showForm && (
                    <button className="btn btn-outline" onClick={() => setShowForm(true)}>
                        + New
                    </button>
                )}
            </header>

            {showForm && (
                <CafeLocationForm
                    mode="create"
                    onSubmit={handleCreate}
                    onCancel={() => { setShowForm(false); setSaveError(null) }}
                    saving={saving}
                    error={saveError}
                />
            )}

            {deleteError && <div className="error-inline">{deleteError}</div>}

            {anyLocated && <CafeMap cafes={cafes} />}

            {cafes.length === 0 ? (
                <div className="empty">No cafés yet. Add your first one above.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search name, city, address…"
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
                                        onClick={() => navigate(`/cafes/${cafe.id}`)}
                                    >
                                        <div className="list-item-main">
                                            <div className="list-item-title">{cafe.name}</div>
                                            <div className="list-item-meta">
                                                {cafe.city ?? 'Unknown city'}
                                                {cafe.rating != null && ` · ${cafe.rating}★`}
                                            </div>
                                        </div>
                                        <button className="brew-del" onClick={(e) => handleDelete(e, cafe)}>Delete</button>
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
