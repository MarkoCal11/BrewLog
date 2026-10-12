import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { useBrewEntries } from '../hooks/useBrewEntries'
import { useCoffeeBeans } from '../hooks/useCoffeeBeans'
import { createBrewEntry, deleteBrewEntry } from '../api/brewEntryApi'
import { BrewEntryForm } from './BrewEntryForm'
import type { BrewEntryFormData } from './BrewEntryForm'
import type { BrewEntry } from '../types/BrewEntry'
import { METHODS, formatMethod } from '../types/BrewEntry'
import { filterSortBrews } from '../utils/listFilters'
import type { BrewSort } from '../utils/listFilters'
import { computeInventory, consumptionByBean } from '../utils/beanInventory'
import { ListToolbar } from './ListToolbar'
import { extractErrorMessage } from '../utils/errors'

export function BrewEntryListComponent() {
    const { data, error, loading, refetch, removeEntry } = useBrewEntries()
    const { data: beans } = useCoffeeBeans()
    const location = useLocation()
    const navigate = useNavigate()

    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [showForm, setShowForm] = useState(false)
    const [cloneValues, setCloneValues] = useState<Partial<BrewEntryFormData> | undefined>(undefined)

    const [search, setSearch] = useState('')
    const [methodFilter, setMethodFilter] = useState('')
    const [beanFilter, setBeanFilter] = useState('')
    const [sort, setSort] = useState<BrewSort>('recent')

    // "Brew again" from a brew detail page lands here with the recipe in router state;
    // the home "Log a brew" CTA lands here with openForm to jump straight into a blank form.
    useEffect(() => {
        const state = location.state as { cloneFrom?: Partial<BrewEntryFormData>; openForm?: boolean } | null
        if (state?.cloneFrom) {
            setCloneValues(state.cloneFrom)
            setShowForm(true)
        } else if (state?.openForm) {
            setShowForm(true)
        } else {
            return
        }
        // Drop the state so a refresh or back-nav doesn't re-trigger the form.
        navigate(location.pathname, { replace: true, state: null })
    }, [location.state, location.pathname, navigate])

    // Estimated remaining grams per bean, for the create form's over-dose caution.
    const remainingByBean = useMemo(() => {
        const consumption = consumptionByBean(data ?? [])
        const map = new Map<number, number>()
        for (const bean of beans ?? []) {
            const remaining = computeInventory(bean, consumption.get(bean.id)).remainingGrams
            if (remaining != null) map.set(bean.id, remaining)
        }
        return map
    }, [data, beans])

    const openBlankForm = () => {
        setCloneValues(undefined)
        setShowForm(true)
    }

    const closeForm = () => {
        setShowForm(false)
        setCloneValues(undefined)
        setSaveError(null)
    }

    const handleCreate = async (formData: BrewEntryFormData) => {
        setSaving(true)
        setSaveError(null)
        try {
            await createBrewEntry({
                method: formData.method,
                grindSize: formData.grindSize,
                waterTempC: formData.waterTempC,
                brewTimeSeconds: formData.brewTimeSeconds,
                doseGrams: formData.doseGrams,
                brewedAt: formData.brewedAt,
                coffeeBeanId: formData.coffeeBeanId,
                waterGrams: formData.waterGrams,
                yieldGrams: formData.yieldGrams,
                rating: formData.rating,
                notes: formData.notes,
            })
            closeForm()
            refetch()
        } catch (err) {
            setSaveError(extractErrorMessage(err, 'Could not save this brew. Please try again.'))
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (entry: BrewEntry) => {
        const name = entry.coffeeBean?.name ?? 'this brew'
        if (!window.confirm(`Delete the brew of "${name}"? This cannot be undone.`)) return
        try {
            removeEntry(entry.id)
            await deleteBrewEntry(entry.id)
        } catch (err) {
            console.error('Failed to delete brew entry:', err)
            refetch()
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const visibleBrews = filterSortBrews(data ?? [], {
        search, method: methodFilter, bean: beanFilter, sort,
    })

    const hasFilters = search !== '' || methodFilter !== '' || beanFilter !== '' || sort !== 'recent'
    const clearFilters = () => {
        setSearch('')
        setMethodFilter('')
        setBeanFilter('')
        setSort('recent')
    }

    // Beans that actually appear in the brew log, for the bean filter dropdown.
    const brewBeans = Array.from(
        new Map((data ?? []).map(e => [e.coffeeBean.id, e.coffeeBean])).values()
    ).sort((a, b) => a.name.localeCompare(b.name))

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

    const goTo = (entry: BrewEntry) => navigate(`/brews/${entry.id}`)

    return (
        <div className="stack-lg">
            <header className="row-between">
                <div>
                    <div className="page-eyebrow">BrewLog</div>
                    <h1 className="page-title">Brews</h1>
                    <p className="page-subtitle">Your brewing log</p>
                </div>
                {!showForm && (
                    <button className="btn btn-outline" onClick={openBlankForm}>
                        + Log
                    </button>
                )}
            </header>

            {showForm && (
                <BrewEntryForm
                    beans={beans ?? []}
                    mode="create"
                    initialValues={cloneValues}
                    onSubmit={handleCreate}
                    onCancel={closeForm}
                    saving={saving}
                    error={saveError}
                    remainingByBean={remainingByBean}
                />
            )}

            {(data ?? []).length === 0 ? (
                <div className="empty">No brews logged yet. Tap "+ Log" to add one.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search bean, method, notes…"
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
                                    const date = new Date(entry.brewedAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })
                                    return (
                                        <li
                                            key={entry.id}
                                            className="list-item list-item-clickable"
                                            onClick={() => goTo(entry)}
                                        >
                                            <div className="list-item-main">
                                                <div className="list-item-title">{entry.coffeeBean?.name}</div>
                                                <div className="list-item-meta">
                                                    {date} · {formatMethod(entry.method)}
                                                    {ratio && ` · ${ratio}`}
                                                    {entry.rating != null && ` · ${entry.rating}★`}
                                                </div>
                                            </div>
                                            <button
                                                className="brew-del"
                                                onClick={e => { e.stopPropagation(); handleDelete(entry) }}
                                            >
                                                Delete
                                            </button>
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
