import { useEffect, useMemo, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import axios from 'axios'
import { useCoffeeBeans } from '../hooks/useCoffeeBeans'
import { useFlavorNotes } from '../hooks/useFlavorNotes'
import { useBrewEntries } from '../hooks/useBrewEntries'
import { addFlavorNoteToBean, createCoffeeBean, deleteCoffeeBean, setBeanFavorite, uploadBeanPhoto } from '../api/coffeeBeanApi'
import type { CoffeeBeanCreateRequest } from '../api/coffeeBeanApi'
import { resolvePhotoUrl } from '../api/config'
import { pickImage } from '../utils/pickImage'
import type { CoffeeBean, ProcessingMethod } from '../types/CoffeeBean'
import { PROCESSING_METHODS, ROAST_LEVELS, formatRoastLevel } from '../types/CoffeeBean'
import { formatFreshness, formatPrice, formatPricePer100g, formatProcessingMethod } from '../utils/beanFormat'
import { extractErrorMessage, BEAN_IN_USE_MESSAGE } from '../utils/errors'
import { filterSortBeans } from '../utils/listFilters'
import type { BeanSort } from '../utils/listFilters'
import { computeInventory, consumptionByBean, stockStatus } from '../utils/beanInventory'
import { ListToolbar } from './ListToolbar'

const STATUS_LABELS = { low: 'Low', empty: 'Empty', finished: 'Finished' } as const

type BeanForm = {
    name: string
    origin: string
    roastLevel: CoffeeBean['roastLevel']
    processingMethod: string
    roasterName: string
    roastDate: string
    price: string
    weightGrams: string
    altitude: string
    tastingNotes: string
}

const emptyForm = (): BeanForm => ({
    name: '',
    origin: '',
    roastLevel: 'MEDIUM',
    processingMethod: '',
    roasterName: '',
    roastDate: '',
    price: '',
    weightGrams: '',
    altitude: '',
    tastingNotes: '',
})

const buildCreateRequest = (form: BeanForm): CoffeeBeanCreateRequest => ({
    name: form.name,
    origin: form.origin,
    roastLevel: form.roastLevel,
    processingMethod: (form.processingMethod as ProcessingMethod) || null,
    roasterName: form.roasterName,
    roastDate: form.roastDate || null,
    price: form.price.trim() ? Number(form.price) : null,
    weightGrams: form.weightGrams.trim() ? Number(form.weightGrams) : null,
    altitude: form.altitude.trim() ? Number(form.altitude) : null,
    tastingNotes: form.tastingNotes.trim() || null,
})

export function CoffeeBeanListComponent() {
    const { data, error, loading, refetch, removeBean, replaceBean } = useCoffeeBeans()
    const { data: flavorNotes } = useFlavorNotes()
    const { data: brews } = useBrewEntries()
    const location = useLocation()
    const navigate = useNavigate()

    const consumptionMap = useMemo(() => consumptionByBean(brews ?? []), [brews])
    const remainingByBean = useMemo(() => {
        const map = new Map<number, number>()
        for (const bean of data ?? []) {
            const remaining = computeInventory(bean, consumptionMap.get(bean.id)).remainingGrams
            if (remaining != null) map.set(bean.id, remaining)
        }
        return map
    }, [data, consumptionMap])

    const [form, setForm] = useState<BeanForm>(emptyForm())
    const [selectedNoteIds, setSelectedNoteIds] = useState<number[]>([])
    // Photo is picked before the bean exists, so we hold the bytes + a preview URL and
    // upload after createCoffeeBean returns the new id (two-step).
    const [photoFile, setPhotoFile] = useState<Blob | null>(null)
    const [photoPreview, setPhotoPreview] = useState<string | null>(null)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [showForm, setShowForm] = useState(false)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    // The home "Add a bean" CTA lands here with openForm to jump straight into a blank form.
    useEffect(() => {
        if ((location.state as { openForm?: boolean } | null)?.openForm) {
            setShowForm(true)
            // Drop the state so a refresh or back-nav doesn't re-trigger the form.
            navigate(location.pathname, { replace: true, state: null })
        }
    }, [location.state, location.pathname, navigate])

    const [search, setSearch] = useState('')
    const [roastFilter, setRoastFilter] = useState('')
    const [methodFilter, setMethodFilter] = useState('')
    const [noteFilter, setNoteFilter] = useState('')
    const [favFilter, setFavFilter] = useState(false)
    const [sort, setSort] = useState<BeanSort>('name')

    const visibleBeans = useMemo(
        () => filterSortBeans(data ?? [], {
            search, roast: roastFilter, method: methodFilter, note: noteFilter, fav: favFilter, sort, remainingByBean,
        }),
        [data, search, roastFilter, methodFilter, noteFilter, favFilter, sort, remainingByBean],
    )

    const hasFilters = search !== '' || roastFilter !== '' || methodFilter !== '' || noteFilter !== '' || favFilter || sort !== 'name'
    const clearFilters = () => {
        setSearch('')
        setRoastFilter('')
        setMethodFilter('')
        setNoteFilter('')
        setFavFilter(false)
        setSort('name')
    }

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value, type } = e.target
        if (type === 'checkbox') {
            const { checked } = e.target as HTMLInputElement
            setForm(prev => ({ ...prev, [name]: checked }))
        } else {
            setForm(prev => ({ ...prev, [name]: value }))
        }
    }

    const toggleNote = (noteId: number) => {
        setSelectedNoteIds(prev =>
            prev.includes(noteId) ? prev.filter(id => id !== noteId) : [...prev, noteId]
        )
    }

    const clearPhoto = () => {
        setPhotoFile(null)
        setPhotoPreview(prev => {
            if (prev) URL.revokeObjectURL(prev)
            return null
        })
    }

    const handlePickPhoto = async () => {
        const file = await pickImage()
        if (!file) return
        setPhotoFile(file)
        setPhotoPreview(prev => {
            if (prev) URL.revokeObjectURL(prev)
            return URL.createObjectURL(file)
        })
    }

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        setSaving(true)
        setSaveError(null)
        try {
            const created = await createCoffeeBean(buildCreateRequest(form))
            await Promise.all(
                selectedNoteIds.map(noteId => addFlavorNoteToBean(created.id, noteId))
            )
            if (photoFile) await uploadBeanPhoto(created.id, photoFile)
            setForm(emptyForm())
            setSelectedNoteIds([])
            clearPhoto()
            setShowForm(false)
            refetch()
        } catch (err) {
            if (axios.isAxiosError(err) && err.response?.status === 409) {
                setSaveError('A bean with this name and roaster already exists.')
            } else {
                setSaveError(extractErrorMessage(err))
            }
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (e: React.MouseEvent, bean: CoffeeBean) => {
        e.stopPropagation()
        if (!window.confirm(`Delete "${bean.name}"? This cannot be undone.`)) return
        setDeleteError(null)
        try {
            removeBean(bean.id)
            await deleteCoffeeBean(bean.id)
        } catch (err) {
            console.error('Failed to delete bean:', err)
            setDeleteError(extractErrorMessage(err, BEAN_IN_USE_MESSAGE))
            refetch()
        }
    }

    const handleToggleFavorite = async (e: React.MouseEvent, bean: CoffeeBean) => {
        e.stopPropagation()
        const optimistic = { ...bean, favorite: !bean.favorite }
        replaceBean(optimistic)
        try {
            const updated = await setBeanFavorite(bean.id, optimistic.favorite)
            replaceBean(updated)
        } catch (err) {
            console.error('Failed to update favorite:', err)
            replaceBean(bean)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    return (
        <div className="stack-lg">
            <header className="row-between">
                <div>
                    <div className="page-eyebrow">BrewLog</div>
                    <h1 className="page-title">Beans</h1>
                    <p className="page-subtitle">Your bean collection</p>
                </div>
                {!showForm && (
                    <button className="btn btn-outline" onClick={() => setShowForm(true)}>
                        + New
                    </button>
                )}
            </header>

            {showForm && (
                <form className="form" onSubmit={handleSubmit}>
                    <p className="form-legend"><span className="required-mark">*</span> Required — all other fields are optional</p>
                    <div className="field">
                        <label className="label">Name <span className="required-mark">*</span></label>
                        <input className="input" name="name" placeholder="e.g. Yirgacheffe" value={form.name} onChange={handleChange} required />
                    </div>
                    <div className="field">
                        <label className="label">Origin <span className="required-mark">*</span></label>
                        <input className="input" name="origin" placeholder="e.g. Ethiopia" value={form.origin} onChange={handleChange} required />
                    </div>
                    <div className="field">
                        <label className="label">Roast level <span className="required-mark">*</span></label>
                        <select className="select" name="roastLevel" value={form.roastLevel} onChange={handleChange}>
                            {ROAST_LEVELS.map(r => (
                                <option key={r} value={r}>{formatRoastLevel(r)}</option>
                            ))}
                        </select>
                    </div>
                    <div className="field">
                        <label className="label">Processing method</label>
                        <select className="select" name="processingMethod" value={form.processingMethod} onChange={handleChange}>
                            <option value="">—</option>
                            {PROCESSING_METHODS.map(pm => (
                                <option key={pm} value={pm}>{formatProcessingMethod(pm)}</option>
                            ))}
                        </select>
                    </div>
                    <div className="field">
                        <label className="label">Roaster <span className="required-mark">*</span></label>
                        <input className="input" name="roasterName" placeholder="e.g. Onyx" value={form.roasterName} onChange={handleChange} required />
                    </div>
                    <div className="field-row">
                        <div className="field">
                            <label className="label">Roast date</label>
                            <input className="input" type="date" name="roastDate" value={form.roastDate} onChange={handleChange} />
                        </div>
                        <div className="field">
                            <label className="label">Altitude (masl)</label>
                            <input className="input" type="number" step="1" min="0" name="altitude" placeholder="e.g. 1900" value={form.altitude} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="field-row">
                        <div className="field">
                            <label className="label">Price (bag total)</label>
                            <input className="input" type="number" step="0.01" min="0" name="price" placeholder="e.g. 18.50" value={form.price} onChange={handleChange} />
                        </div>
                        <div className="field">
                            <label className="label">Bag weight (g)</label>
                            <input className="input" type="number" step="1" min="0" name="weightGrams" placeholder="e.g. 250" value={form.weightGrams} onChange={handleChange} />
                        </div>
                    </div>
                    <div className="field">
                        <label className="label">Tasting notes</label>
                        <textarea className="input" name="tastingNotes" value={form.tastingNotes} onChange={handleChange} maxLength={255} rows={3} placeholder="Roaster's notes, your impressions..." />
                        {form.tastingNotes.length > 0 && (
                            <span className="field-hint">{form.tastingNotes.length}/255</span>
                        )}
                    </div>
                    {(flavorNotes ?? []).length > 0 && (
                        <div className="field">
                            <label className="label">Flavor notes</label>
                            <div className="chip-row">
                                {(flavorNotes ?? []).map(note => {
                                    const selected = selectedNoteIds.includes(note.id)
                                    return (
                                        <button
                                            key={note.id}
                                            type="button"
                                            className={selected ? 'chip chip-toggle chip-toggle-active' : 'chip chip-toggle'}
                                            onClick={() => toggleNote(note.id)}
                                        >
                                            {note.name}
                                            <span className="chip-meta">{note.category}</span>
                                        </button>
                                    )
                                })}
                            </div>
                        </div>
                    )}
                    <div className="field">
                        <label className="label">Photo</label>
                        {photoPreview && (
                            <img className="bean-photo bean-photo-preview" src={photoPreview} alt="Selected bean photo" />
                        )}
                        <div className="field-row">
                            <button type="button" className="btn btn-ghost btn-block" onClick={handlePickPhoto}>
                                {photoPreview ? 'Change photo' : 'Add photo'}
                            </button>
                            {photoPreview && (
                                <button type="button" className="btn btn-ghost btn-block" onClick={clearPhoto}>
                                    Remove
                                </button>
                            )}
                        </div>
                    </div>
                    <div className="field-row" style={{ marginTop: 4 }}>
                        <button type="button" className="btn btn-ghost btn-block" onClick={() => { clearPhoto(); setSaveError(null); setShowForm(false) }}>Cancel</button>
                        <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                            {saving ? 'Saving...' : 'Add Bean'}
                        </button>
                    </div>
                    {saveError && <div className="error-inline">{saveError}</div>}
                </form>
            )}

            {deleteError && <div className="error-inline">{deleteError}</div>}

            {(data ?? []).length === 0 ? (
                <div className="empty">No beans yet. Add your first one above.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search name, origin, roaster, flavor…"
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
                                <option value="remaining">Least left</option>
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
                        {(flavorNotes ?? []).length > 0 && (
                            <label className="control">
                                <span>Flavor</span>
                                <select className="select" value={noteFilter} onChange={e => setNoteFilter(e.target.value)}>
                                    <option value="">All</option>
                                    {(flavorNotes ?? []).map(note => (
                                        <option key={note.id} value={note.id}>{note.name}</option>
                                    ))}
                                </select>
                            </label>
                        )}
                        <label className="control">
                            <span>Favorites</span>
                            <select className="select" value={favFilter ? 'fav' : ''} onChange={e => setFavFilter(e.target.value === 'fav')}>
                                <option value="">All</option>
                                <option value="fav">★ only</option>
                            </select>
                        </label>
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
                        const status = stockStatus(bean, computeInventory(bean, consumptionMap.get(bean.id)))
                        return (
                            <li
                                key={bean.id}
                                className="list-item list-item-clickable"
                                onClick={() => navigate(`/details/${bean.id}`)}
                            >
                                <button
                                    className={bean.favorite ? 'star-btn star-btn-active' : 'star-btn'}
                                    onClick={(e) => handleToggleFavorite(e, bean)}
                                    aria-label={bean.favorite ? 'Unfavorite' : 'Favorite'}
                                    title={bean.favorite ? 'Unfavorite' : 'Favorite'}
                                >
                                    {bean.favorite ? '★' : '☆'}
                                </button>
                                {bean.photoUrl
                                    ? <img className="list-thumb" src={resolvePhotoUrl(bean.photoUrl)!} alt="" />
                                    : <div className="list-thumb" />}
                                <div className="list-item-main">
                                    <div className="list-item-title">{bean.name}</div>
                                    <div className="list-item-meta">
                                        {bean.origin} · {formatRoastLevel(bean.roastLevel).toLowerCase()}
                                        {bean.brewCount > 0 && ` · ${bean.averageRating.toFixed(1)}★`}
                                        {freshness && ` · ${freshness}`}
                                        {price && ` · ${price}`}
                                    </div>
                                </div>
                                {status && <span className={`stock-badge stock-badge-${status}`}>{STATUS_LABELS[status]}</span>}
                                <button className="brew-del" onClick={(e) => handleDelete(e, bean)}>Delete</button>
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
