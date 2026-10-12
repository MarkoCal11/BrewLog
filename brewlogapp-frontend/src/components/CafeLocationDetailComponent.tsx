import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router'
import { getCafeLocation, updateCafeLocation, deleteCafeLocation } from '../api/cafeLocationApi'
import type { CafeLocationCreateRequest } from '../api/cafeLocationApi'
import type { CafeLocation } from '../types/CafeLocation'
import { hasCoords, instagramUrl, instagramLabel } from '../types/CafeLocation'
import { extractErrorMessage } from '../utils/errors'
import { CafeLocationForm } from './CafeLocationForm'
import { CafeMap } from './CafeMap'
import { StarDisplay } from './StarPicker'

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

export function CafeLocationDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [cafe, setCafe] = useState<CafeLocation | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    useEffect(() => {
        getCafeLocation(Number(id))
            .then(setCafe)
            .catch(err => setError(err))
            .finally(() => setLoading(false))
    }, [id])

    const handleEditSubmit = async (data: CafeLocationCreateRequest) => {
        setSaving(true)
        setSaveError(null)
        try {
            const updated = await updateCafeLocation(Number(id), data)
            setCafe(updated)
            setEditing(false)
        } catch (err) {
            setSaveError(extractErrorMessage(err))
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async () => {
        if (!cafe) return
        if (!window.confirm(`Delete "${cafe.name}"? This cannot be undone.`)) return
        setDeleteError(null)
        try {
            await deleteCafeLocation(cafe.id)
            navigate('/cafes')
        } catch (err) {
            console.error('Failed to delete café:', err)
            setDeleteError(extractErrorMessage(err))
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!cafe) return <div className="empty">Café not found.</div>

    const igUrl = instagramUrl(cafe.instagram)

    return (
        <div className="stack-lg">
            <Link to="/cafes" className="back-link">Cafés</Link>

            {editing ? (
                <>
                    <h1 className="page-title">Edit Café</h1>
                    <CafeLocationForm
                        mode="edit"
                        cafe={cafe}
                        onSubmit={handleEditSubmit}
                        onCancel={() => setEditing(false)}
                        saving={saving}
                        error={saveError}
                    />
                </>
            ) : (
                <>
                    <header className="row-between">
                        <div>
                            <div className="detail-eyebrow">Café</div>
                            <h1 className="page-title" style={{ marginTop: 8 }}>{cafe.name}</h1>
                            {cafe.city && <div className="detail-sub">{cafe.city}</div>}
                        </div>
                        <div className="row-actions detail-actions">
                            <button className="btn btn-ghost" onClick={() => { setSaveError(null); setEditing(true) }}>Edit</button>
                            <button className="btn btn-danger" onClick={handleDelete}>Delete</button>
                        </div>
                    </header>

                    {deleteError && <div className="error-inline">{deleteError}</div>}

                    {/* Body: map anchor beside the details spec sheet */}
                    <div className="detail-body-grid">
                        <div className="detail-main">
                            {hasCoords(cafe)
                                ? <CafeMap cafes={[cafe]} linkToDetail={false} />
                                : <div className="empty">No map location set for this café.</div>}
                        </div>
                        <div className="detail-side">
                            <div className="section-title">Details</div>
                            <div className="spec-list">
                                <Spec k="Rating">
                                    {cafe.rating != null ? <StarDisplay value={cafe.rating} /> : '—'}
                                </Spec>
                                {cafe.city && <Spec k="City">{cafe.city}</Spec>}
                                {cafe.address && <Spec k="Address">{cafe.address}</Spec>}
                                {igUrl && (
                                    <Spec k="Instagram">
                                        <a href={igUrl} target="_blank" rel="noopener noreferrer">
                                            {instagramLabel(cafe.instagram as string)}
                                        </a>
                                    </Spec>
                                )}
                                {hasCoords(cafe) && (
                                    <Spec k="Coordinates">
                                        <span className="cafe-coords">
                                            {(cafe.latitude as number).toFixed(5)}, {(cafe.longitude as number).toFixed(5)}
                                        </span>
                                    </Spec>
                                )}
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
