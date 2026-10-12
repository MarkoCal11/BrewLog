import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router'
import { getCafeLocationGlobal, updateCafeLocationGlobal, deleteCafeLocationGlobal } from '../api/cafeLocationApi'
import type { CafeLocationCreateRequest } from '../api/cafeLocationApi'
import type { CafeLocation } from '../types/CafeLocation'
import { hasCoords, instagramUrl, instagramLabel } from '../types/CafeLocation'
import { extractErrorMessage } from '../utils/errors'
import { CafeLocationForm } from './CafeLocationForm'
import { CafeMap } from './CafeMap'
import { StarDisplay } from './StarPicker'

function Spec({ k, children }: { k: string; children: React.ReactNode }) {
    return (
        <div className="spec">
            <span className="spec-k">{k}</span>
            <span className="spec-lead" />
            <span className="spec-v">{children}</span>
        </div>
    )
}

export function AdminCafeDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [cafe, setCafe] = useState<CafeLocation | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [deleting, setDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    useEffect(() => {
        getCafeLocationGlobal(Number(id))
            .then(setCafe)
            .catch(setError)
            .finally(() => setLoading(false))
    }, [id])

    const handleEditSubmit = async (data: CafeLocationCreateRequest) => {
        setSaving(true)
        setSaveError(null)
        try {
            const updated = await updateCafeLocationGlobal(Number(id), data)
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
        if (!window.confirm(`Delete "${cafe.name}"${cafe.ownerUsername ? ` (owned by ${cafe.ownerUsername})` : ''}? This cannot be undone.`)) return
        setDeleting(true)
        setDeleteError(null)
        try {
            await deleteCafeLocationGlobal(cafe.id)
            navigate('/admin/cafes')
        } catch (err) {
            setDeleteError(extractErrorMessage(err))
            setDeleting(false)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!cafe) return <div className="empty">Café not found.</div>

    const igUrl = instagramUrl(cafe.instagram)

    return (
        <div className="stack-lg">
            <Link to="/admin/cafes" className="back-link">All Cafés</Link>

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
                            <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                                {deleting ? 'Deleting…' : 'Delete'}
                            </button>
                        </div>
                    </header>

                    {deleteError && <div className="error-inline">{deleteError}</div>}

                    <div className="detail-body-grid">
                        <div className="detail-main">
                            {hasCoords(cafe)
                                ? <CafeMap cafes={[cafe]} linkToDetail={false} />
                                : <div className="empty">No map location set for this café.</div>}
                        </div>
                        <div className="detail-side">
                            <div>
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
                                    {cafe.ownerUsername && <Spec k="Owner">{cafe.ownerUsername}</Spec>}
                                </div>
                            </div>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
