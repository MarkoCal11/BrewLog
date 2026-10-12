import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router'
import axios from 'axios'
import { getCoffeeBeanGlobal, updateCoffeeBeanGlobal, deleteCoffeeBeanGlobal, deleteBeanPhotoGlobal } from '../api/coffeeBeanApi'
import type { CoffeeBeanUpdateRequest } from '../api/coffeeBeanApi'
import type { CoffeeBean } from '../types/CoffeeBean'
import { formatRoastLevel } from '../types/CoffeeBean'
import { resolvePhotoUrl } from '../api/config'
import { formatAltitude, formatFreshness, formatPrice, formatPricePer100g, formatProcessingMethod } from '../utils/beanFormat'
import { extractErrorMessage, BEAN_IN_USE_MESSAGE } from '../utils/errors'
import { BeanEditForm } from './BeanEditForm'

function Spec({ k, children }: { k: string; children: React.ReactNode }) {
    return (
        <div className="spec">
            <span className="spec-k">{k}</span>
            <span className="spec-lead" />
            <span className="spec-v">{children}</span>
        </div>
    )
}

export function AdminBeanDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [bean, setBean] = useState<CoffeeBean | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [deleting, setDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState<string | null>(null)
    const [photoBusy, setPhotoBusy] = useState(false)
    const [photoError, setPhotoError] = useState<string | null>(null)

    useEffect(() => {
        getCoffeeBeanGlobal(Number(id))
            .then(setBean)
            .catch(setError)
            .finally(() => setLoading(false))
    }, [id])

    const handleEditSubmit = async (data: CoffeeBeanUpdateRequest) => {
        setSaving(true)
        setSaveError(null)
        try {
            const updated = await updateCoffeeBeanGlobal(Number(id), data)
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

    const handleDelete = async () => {
        if (!bean) return
        if (!window.confirm(`Delete "${bean.name}"${bean.ownerUsername ? ` (owned by ${bean.ownerUsername})` : ''}? This cannot be undone.`)) return
        setDeleting(true)
        setDeleteError(null)
        try {
            await deleteCoffeeBeanGlobal(bean.id)
            navigate('/admin/beans')
        } catch (err) {
            setDeleteError(extractErrorMessage(err, BEAN_IN_USE_MESSAGE))
            setDeleting(false)
        }
    }

    const handleRemovePhoto = async () => {
        if (!bean) return
        if (!window.confirm(`Remove this photo${bean.ownerUsername ? ` (owned by ${bean.ownerUsername})` : ''}?`)) return
        setPhotoBusy(true)
        setPhotoError(null)
        try {
            await deleteBeanPhotoGlobal(bean.id)
            setBean({ ...bean, photoUrl: null })
        } catch (err) {
            setPhotoError(extractErrorMessage(err))
        } finally {
            setPhotoBusy(false)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!bean) return <div className="empty">Bean not found.</div>

    const pricePer100g = formatPricePer100g(bean.price, bean.weightGrams)

    return (
        <div className="stack-lg">
            <Link to="/admin/beans" className="back-link">All Beans</Link>

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
                </>
            ) : (
                <>
                    <div className="detail-hero">
                        <div className="detail-figure">
                            {bean.photoUrl
                                ? <img className="bean-photo" src={resolvePhotoUrl(bean.photoUrl)!} alt={bean.name} />
                                : <div className="bean-photo-placeholder">bean photo</div>}
                            <div className="detail-figure-actions">
                                {bean.photoUrl && (
                                    <button className="btn btn-ghost" onClick={handleRemovePhoto} disabled={photoBusy}>
                                        {photoBusy ? 'Removing…' : 'Remove photo'}
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
                                <div className="row-actions detail-actions" style={{ marginLeft: 'auto' }}>
                                    <button className="btn btn-ghost" onClick={() => { setSaveError(null); setEditing(true) }}>Edit</button>
                                    <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                                        {deleting ? 'Deleting…' : 'Delete'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {deleteError && <div className="error-inline">{deleteError}</div>}

                    <div className="detail-body-grid">
                        <div className="detail-main">
                            {bean.tastingNotes && (
                                <div>
                                    <div className="section-title">Tasting notes</div>
                                    <p className="detail-prose">{bean.tastingNotes}</p>
                                </div>
                            )}
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
                                    {bean.ownerUsername && <Spec k="Owner">{bean.ownerUsername}</Spec>}
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
