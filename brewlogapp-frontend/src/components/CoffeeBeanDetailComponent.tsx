import { useState, useEffect, useMemo, lazy, Suspense } from 'react'
import { Link, useParams } from 'react-router'
import axios from 'axios'
import { addFlavorNoteToBean, removeFlavorNoteFromBean, getCoffeeBean, updateCoffeeBean, setBeanFavorite, setBeanFinished, uploadBeanPhoto, deleteBeanPhoto } from '../api/coffeeBeanApi'
import type { CoffeeBeanUpdateRequest } from '../api/coffeeBeanApi'
import { resolvePhotoUrl } from '../api/config'
import { pickImage } from '../utils/pickImage'
import { getAllFlavorNotes, getFlavorNotesForBean } from '../api/flavorNoteApi'
import type { CoffeeBean } from '../types/CoffeeBean'
import { formatRoastLevel } from '../types/CoffeeBean'
import type { FlavorNote } from '../types/FlavorNote'
import { formatCategory } from '../types/FlavorNote'

const BeanFlavorRadar = lazy(() =>
    import('./BeanFlavorRadar').then(m => ({ default: m.BeanFlavorRadar })),
)
import { useBrewEntries } from '../hooks/useBrewEntries'
import { extractErrorMessage } from '../utils/errors'
import { formatAltitude, formatFreshness, formatPrice, formatPricePer100g, formatProcessingMethod } from '../utils/beanFormat'
import { computeInventory, consumptionByBean, formatGrams } from '../utils/beanInventory'
import { BeanEditForm } from './BeanEditForm'

// A single dot-leader spec row: label … value
function Spec({ k, children }: { k: string; children: React.ReactNode }) {
    return (
        <div className="spec">
            <span className="spec-k">{k}</span>
            <span className="spec-lead" />
            <span className="spec-v">{children}</span>
        </div>
    )
}

export function CoffeeBeanDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const [bean, setBean] = useState<CoffeeBean | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)
    const [flavorNotes, setFlavorNotes] = useState<FlavorNote[]>([])
    const [notesError, setNotesError] = useState<Error | null>(null)
    const [allNotes, setAllNotes] = useState<FlavorNote[]>([])
    const [togglingId, setTogglingId] = useState<number | null>(null)
    const [noteError, setNoteError] = useState<string | null>(null)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [photoBusy, setPhotoBusy] = useState(false)
    const [photoError, setPhotoError] = useState<string | null>(null)
    const { data: brews } = useBrewEntries()
    const consumptionMap = useMemo(() => consumptionByBean(brews ?? []), [brews])

    // Tasting profile radar: a stable wheel of every known category (seeded from all
    // notes so the polygon keeps its shape), with this bean's notes tallied on top.
    const flavorRadar = useMemo(() => {
        const counts = new Map<string, number>()
        for (const note of allNotes) if (!counts.has(note.category)) counts.set(note.category, 0)
        for (const note of flavorNotes) counts.set(note.category, (counts.get(note.category) ?? 0) + 1)
        return [...counts.entries()].map(([category, count]) => ({ category: formatCategory(category), count }))
    }, [flavorNotes, allNotes])

    useEffect(() => {
        getCoffeeBean(Number(id))
            .then(setBean)
            .catch(err => setError(err))
            .finally(() => setLoading(false))
    }, [id])

    useEffect(() => {
        if (!id) return
        getFlavorNotesForBean(Number(id))
            .then(setFlavorNotes)
            .catch(setNotesError)
    }, [id])

    useEffect(() => {
        getAllFlavorNotes().then(setAllNotes).catch(() => {})
    }, [])

    const startEditing = () => {
        if (!bean) return
        setSaveError(null)
        setEditing(true)
    }

    const handleEditSubmit = async (data: CoffeeBeanUpdateRequest) => {
        setSaving(true)
        setSaveError(null)
        try {
            const updated = await updateCoffeeBean(Number(id), data)
            setBean(updated)
            setEditing(false)
        } catch (err) {
            if (axios.isAxiosError(err) && err.response?.status === 409) {
                setSaveError('A bean with this name and roaster already exists.')
            } else {
                setSaveError(err instanceof Error ? err.message : 'Failed to save changes.')
            }
        } finally {
            setSaving(false)
        }
    }

    const handleToggleFavorite = async () => {
        if (!bean) return
        const next = !bean.favorite
        setBean({ ...bean, favorite: next })
        try {
            const updated = await setBeanFavorite(bean.id, next)
            setBean(updated)
        } catch (err) {
            console.error('Failed to update favorite:', err)
            setBean(bean)
        }
    }

    const handleToggleFinished = async () => {
        if (!bean) return
        const next = !bean.finished
        setBean({ ...bean, finished: next })
        try {
            const updated = await setBeanFinished(bean.id, next)
            setBean(updated)
        } catch (err) {
            console.error('Failed to update finished:', err)
            setBean(bean)
        }
    }

    // Photo: add/replace via multipart upload, remove via delete. Both act immediately
    // (independent of the bean form's Save), mirroring the favorite/finished toggles.
    const handlePickPhoto = async () => {
        if (!bean) return
        const file = await pickImage()
        if (!file) return
        setPhotoBusy(true)
        setPhotoError(null)
        try {
            const updated = await uploadBeanPhoto(bean.id, file)
            setBean(updated)
        } catch (err) {
            console.error('Failed to upload photo:', err)
            setPhotoError(extractErrorMessage(err))
        } finally {
            setPhotoBusy(false)
        }
    }

    const handleRemovePhoto = async () => {
        if (!bean) return
        if (!window.confirm('Remove this photo?')) return
        setPhotoBusy(true)
        setPhotoError(null)
        try {
            await deleteBeanPhoto(bean.id)
            setBean({ ...bean, photoUrl: null })
        } catch (err) {
            console.error('Failed to remove photo:', err)
            setPhotoError(extractErrorMessage(err))
        } finally {
            setPhotoBusy(false)
        }
    }

    // Attach/detach a flavor note (edit mode). Optimistic, with revert on failure.
    const toggleNote = async (note: FlavorNote) => {
        if (!id) return
        const attached = flavorNotes.some(f => f.id === note.id)
        setTogglingId(note.id)
        setNoteError(null)
        setFlavorNotes(prev => attached ? prev.filter(f => f.id !== note.id) : [...prev, note])
        try {
            if (attached) await removeFlavorNoteFromBean(Number(id), note.id)
            else await addFlavorNoteToBean(Number(id), note.id)
        } catch (err) {
            console.error('Failed to update flavor note:', err)
            setFlavorNotes(prev => attached ? [...prev, note] : prev.filter(f => f.id !== note.id))
            setNoteError(extractErrorMessage(err))
        } finally {
            setTogglingId(null)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!bean) return <div className="empty">Bean not found.</div>

    const consumption = consumptionMap.get(bean.id)
    const inv = computeInventory(bean, consumption)
    const pricePer100g = formatPricePer100g(bean.price, bean.weightGrams)

    return (
        <div className="stack-lg">
            <Link to="/beans" className="back-link">Beans</Link>

            {editing ? (
                <>
                    <h1 className="page-title">Edit Bean</h1>
                    <BeanEditForm
                        bean={bean}
                        onSubmit={handleEditSubmit}
                        onCancel={() => setEditing(false)}
                        saving={saving}
                        error={saveError}
                    />

                    {/* Edit mode: tap any note to attach/detach (active = attached). */}
                    <div>
                        <div className="section-title">Flavor Notes</div>
                        {notesError && <div className="error-inline">Error loading flavor notes: {notesError.message}</div>}
                        {allNotes.length === 0 ? (
                            <div className="empty">No flavor notes available. Create some on the Flavors page.</div>
                        ) : (
                            <div className="chip-row">
                                {allNotes.map(note => {
                                    const attached = flavorNotes.some(f => f.id === note.id)
                                    return (
                                        <button
                                            key={note.id}
                                            type="button"
                                            className={attached ? 'chip chip-toggle chip-toggle-active' : 'chip chip-toggle'}
                                            onClick={() => toggleNote(note)}
                                            disabled={togglingId === note.id}
                                        >
                                            {note.name}
                                            <span className="chip-meta">{note.category}</span>
                                        </button>
                                    )
                                })}
                            </div>
                        )}
                        {noteError && <div className="error-inline">{noteError}</div>}
                    </div>
                </>
            ) : (
                <>
                    {/* Hero: photo figure beside the headline + rating */}
                    <div className="detail-hero">
                        <div className="detail-figure">
                            {bean.photoUrl ? (
                                <img className="bean-photo" src={resolvePhotoUrl(bean.photoUrl)!} alt={bean.name} />
                            ) : (
                                <div className="bean-photo-placeholder">bean photo</div>
                            )}
                            <div className="detail-figure-actions">
                                <button className="btn btn-ghost" onClick={handlePickPhoto} disabled={photoBusy}>
                                    {photoBusy ? 'Uploading…' : bean.photoUrl ? 'Change photo' : 'Add photo'}
                                </button>
                                {bean.photoUrl && (
                                    <button className="btn btn-ghost" onClick={handleRemovePhoto} disabled={photoBusy}>
                                        Remove
                                    </button>
                                )}
                            </div>
                            {photoError && <div className="error-inline">{photoError}</div>}
                        </div>

                        <div>
                            <div className="detail-eyebrow">
                                {bean.origin}{bean.processingMethod && ` · ${formatProcessingMethod(bean.processingMethod)}`}
                            </div>
                            <h1 className="page-title" style={{ marginTop: 8 }}>{bean.name}</h1>
                            <div className="detail-sub">{bean.roasterName}</div>
                            <div className="detail-rating">
                                <div className="detail-rating-big">
                                    {bean.brewCount > 0 ? <b>{bean.averageRating.toFixed(1)}★</b> : '—'}
                                </div>
                                <div className="detail-rating-meta">
                                    {bean.brewCount > 0
                                        ? `average over ${bean.brewCount} brew${bean.brewCount === 1 ? '' : 's'}`
                                        : 'no rated brews yet'}
                                </div>
                                <div className="row-actions" style={{ marginLeft: 'auto' }}>
                                    <button
                                        className={bean.favorite ? 'star-btn star-btn-active' : 'star-btn'}
                                        onClick={handleToggleFavorite}
                                        aria-label={bean.favorite ? 'Unfavorite' : 'Favorite'}
                                        title={bean.favorite ? 'Unfavorite' : 'Favorite'}
                                    >
                                        {bean.favorite ? '★' : '☆'}
                                    </button>
                                    <button className="btn btn-ghost" onClick={startEditing}>Edit</button>
                                </div>
                            </div>
                            {flavorNotes.length > 0 && (
                                <div className="chip-row" style={{ marginTop: 16 }}>
                                    {flavorNotes.map(note => (
                                        <span key={note.id} className="chip">{note.name}</span>
                                    ))}
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Body: tasting notes + flavor profile | specs + inventory */}
                    <div className="detail-body-grid">
                        <div className="detail-main">
                            {bean.tastingNotes && (
                                <div>
                                    <div className="section-title">Tasting notes</div>
                                    <p className="detail-prose">{bean.tastingNotes}</p>
                                </div>
                            )}

                            <div>
                                <div className="section-title">Flavor notes</div>
                                {notesError && <div className="error-inline">Error loading flavor notes: {notesError.message}</div>}
                                {flavorNotes.length === 0 ? (
                                    <div className="empty">No flavor notes yet.</div>
                                ) : (
                                    <>
                                        {flavorNotes.length >= 3 && flavorRadar.length >= 3 && (
                                            <div className="card" style={{ marginBottom: 12 }}>
                                                <div className="card-title">Tasting profile</div>
                                                <Suspense fallback={<div className="empty">Loading chart…</div>}>
                                                    <BeanFlavorRadar data={flavorRadar} />
                                                </Suspense>
                                            </div>
                                        )}
                                        <div className="chip-row">
                                            {flavorNotes.map(note => (
                                                <span key={note.id} className="chip">
                                                    {note.name}
                                                    <span className="chip-meta">{note.category}</span>
                                                </span>
                                            ))}
                                        </div>
                                    </>
                                )}
                                {noteError && <div className="error-inline">{noteError}</div>}
                            </div>
                        </div>

                        <div className="detail-side">
                            <div>
                                <div className="section-title">Specs</div>
                                <div className="spec-list">
                                    <Spec k="Roast level">{formatRoastLevel(bean.roastLevel)}</Spec>
                                    {bean.processingMethod && <Spec k="Processing">{formatProcessingMethod(bean.processingMethod)}</Spec>}
                                    <Spec k="Origin">{bean.origin}</Spec>
                                    {bean.altitude != null && <Spec k="Altitude">{formatAltitude(bean.altitude)}</Spec>}
                                    {bean.roastDate && (
                                        <Spec k="Roast date">
                                            {bean.roastDate}{formatFreshness(bean.roastDate) && ` · ${formatFreshness(bean.roastDate)}`}
                                        </Spec>
                                    )}
                                    {bean.price != null && (
                                        <Spec k="Price">
                                            {formatPrice(bean.price)}{bean.weightGrams != null && ` · ${bean.weightGrams}g`}
                                        </Spec>
                                    )}
                                    {pricePer100g && <Spec k="Per 100g">{pricePer100g}</Spec>}
                                </div>
                            </div>

                            <div>
                                <div className="row-between">
                                    <div className="section-title">Inventory</div>
                                    <button className="btn btn-ghost" onClick={handleToggleFinished}>
                                        {bean.finished ? 'Mark active' : 'Mark finished'}
                                    </button>
                                </div>
                                {bean.finished ? (
                                    <div className="empty">This bag is marked finished.</div>
                                ) : inv.bagGrams != null ? (
                                    <div className="card">
                                        <div className="card-body stack-sm">
                                            <div className="inv-bar">
                                                <div
                                                    className={inv.low ? 'inv-bar-fill is-low' : 'inv-bar-fill'}
                                                    style={{ width: `${inv.percentRemaining ?? 0}%` }}
                                                />
                                            </div>
                                            <div className="detail-row">
                                                <span className="detail-label">Remaining</span>
                                                <span className="detail-value">
                                                    {formatGrams(inv.remainingGrams ?? 0)} of {formatGrams(inv.bagGrams)}
                                                    {inv.percentRemaining != null && ` · ${Math.round(inv.percentRemaining)}%`}
                                                </span>
                                            </div>
                                            {inv.brewsLeft != null && !inv.empty && (
                                                <div className="detail-row">
                                                    <span className="detail-label">Brews left</span>
                                                    <span className="detail-value">~{inv.brewsLeft}</span>
                                                </div>
                                            )}
                                            <div className="detail-row">
                                                <span className="detail-label">Used</span>
                                                <span className="detail-value">
                                                    {formatGrams(inv.consumedGrams)} over {consumption?.count ?? 0} brew{(consumption?.count ?? 0) === 1 ? '' : 's'}
                                                </span>
                                            </div>
                                            {inv.empty && <div className="error-inline">Out of beans — mark finished?</div>}
                                            {inv.low && <div className="field-hint" style={{ textAlign: 'left' }}>Running low.</div>}
                                        </div>
                                    </div>
                                ) : (
                                    <div className="empty">Add a bag weight to track how much is left.</div>
                                )}
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
