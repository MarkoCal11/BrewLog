import { useState, useEffect } from 'react'
import { Link, useParams, useNavigate } from 'react-router'
import { getBrewEntryGlobal, updateBrewEntryGlobal, deleteBrewEntryGlobal } from '../api/brewEntryApi'
import { BrewEntryForm } from './BrewEntryForm'
import type { BrewEntryFormData } from './BrewEntryForm'
import type { BrewEntry } from '../types/BrewEntry'
import { brewCost, formatPrice } from '../utils/beanFormat'
import { extractErrorMessage } from '../utils/errors'

const isEspresso = (method: string) => method === 'ESPRESSO'

function formatTime(seconds: number) {
    const m = Math.floor(seconds / 60)
    const s = seconds % 60
    return `${m}:${String(s).padStart(2, '0')}`
}

function formatRatio(entry: BrewEntry) {
    if (!entry.doseGrams) return null
    if (isEspresso(entry.method) && entry.yieldGrams) {
        return `1:${(entry.yieldGrams / entry.doseGrams).toFixed(1)}`
    }
    if (entry.waterGrams) {
        return `1:${(entry.waterGrams / entry.doseGrams).toFixed(1)}`
    }
    return null
}

function Spec({ k, children }: { k: string; children: React.ReactNode }) {
    return (
        <div className="spec">
            <span className="spec-k">{k}</span>
            <span className="spec-lead" />
            <span className="spec-v">{children}</span>
        </div>
    )
}

export function AdminBrewDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const navigate = useNavigate()
    const [entry, setEntry] = useState<BrewEntry | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)
    const [editing, setEditing] = useState(false)
    const [saving, setSaving] = useState(false)
    const [deleting, setDeleting] = useState(false)
    const [deleteError, setDeleteError] = useState<string | null>(null)

    useEffect(() => {
        getBrewEntryGlobal(Number(id))
            .then(setEntry)
            .catch(setError)
            .finally(() => setLoading(false))
    }, [id])

    const handleUpdate = async (formData: BrewEntryFormData) => {
        setSaving(true)
        try {
            const updated = await updateBrewEntryGlobal(Number(id), {
                method: formData.method,
                grindSize: formData.grindSize,
                waterTempC: formData.waterTempC,
                brewTimeSeconds: formData.brewTimeSeconds,
                doseGrams: formData.doseGrams,
                waterGrams: formData.waterGrams,
                yieldGrams: formData.yieldGrams,
                rating: formData.rating,
                notes: formData.notes,
                brewedAt: formData.brewedAt,
                coffeeBeanId: formData.coffeeBeanId,
            })
            setEntry(updated)
            setEditing(false)
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async () => {
        if (!entry) return
        const name = entry.coffeeBean?.name ?? 'this brew'
        if (!window.confirm(`Delete brew of "${name}"${entry.ownerUsername ? ` (owned by ${entry.ownerUsername})` : ''}? This cannot be undone.`)) return
        setDeleting(true)
        setDeleteError(null)
        try {
            await deleteBrewEntryGlobal(entry.id)
            navigate('/admin/brews')
        } catch (err) {
            setDeleteError(extractErrorMessage(err))
            setDeleting(false)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!entry) return <div className="empty">Brew not found.</div>

    const ratio = formatRatio(entry)
    const cost = brewCost(entry.doseGrams, entry.coffeeBean?.price, entry.coffeeBean?.weightGrams)

    if (editing) {
        return (
            <div className="stack-lg">
                <Link to="/admin/brews" className="back-link">All Brews</Link>
                <h1 className="page-title">Edit Brew</h1>
                <BrewEntryForm
                    mode="edit"
                    initialValues={{
                        method: entry.method,
                        grindSize: entry.grindSize,
                        waterTempC: entry.waterTempC,
                        brewTimeSeconds: entry.brewTimeSeconds,
                        doseGrams: entry.doseGrams ?? 0,
                        waterGrams: entry.waterGrams,
                        yieldGrams: entry.yieldGrams,
                        rating: entry.rating,
                        notes: entry.notes,
                        brewedAt: entry.brewedAt,
                        coffeeBeanId: entry.coffeeBean?.id ?? 0,
                    }}
                    onSubmit={handleUpdate}
                    onCancel={() => setEditing(false)}
                    saving={saving}
                />
            </div>
        )
    }

    const recipe = (
        <div>
            <div className="section-title">Recipe</div>
            <div className="spec-list">
                {entry.doseGrams != null && <Spec k="Dose">{entry.doseGrams}g</Spec>}
                {isEspresso(entry.method)
                    ? entry.yieldGrams != null && <Spec k="Yield">{entry.yieldGrams}g</Spec>
                    : entry.waterGrams != null && <Spec k="Water">{entry.waterGrams}g</Spec>}
                {entry.brewTimeSeconds != null && <Spec k="Brew time">{formatTime(entry.brewTimeSeconds)}</Spec>}
                {entry.grindSize != null && <Spec k="Grind size">{entry.grindSize}</Spec>}
                {entry.waterTempC != null && <Spec k="Water temp">{entry.waterTempC}°C</Spec>}
                {cost != null && <Spec k="Cost">{formatPrice(cost)}</Spec>}
                {entry.ownerUsername && <Spec k="Owner">{entry.ownerUsername}</Spec>}
            </div>
        </div>
    )

    return (
        <div className="stack-lg">
            <Link to="/admin/brews" className="back-link">All Brews</Link>

            <div className="detail-hero">
                <div className="detail-figure">
                    <div className="brew-scorecard">
                        <div className="brew-score-big">{entry.rating != null ? `${entry.rating}★` : '—'}</div>
                        <div className="brew-score-cap">rating</div>
                        <div className="brew-score-ratio">{ratio ?? '—'}</div>
                        <div className="brew-score-cap">ratio</div>
                    </div>
                </div>
                <div>
                    <div className="detail-eyebrow">{entry.method.replace(/_/g, ' ')}</div>
                    <h1 className="page-title" style={{ marginTop: 8 }}>{entry.coffeeBean?.name}</h1>
                    <div className="detail-sub">{new Date(entry.brewedAt).toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' })}</div>
                    <div className="row-actions detail-actions" style={{ marginTop: 18 }}>
                        <button className="btn btn-ghost" onClick={() => setEditing(true)}>Edit</button>
                        <button className="btn btn-danger" onClick={handleDelete} disabled={deleting}>
                            {deleting ? 'Deleting…' : 'Delete'}
                        </button>
                    </div>
                </div>
            </div>

            {deleteError && <div className="error-inline">{deleteError}</div>}

            {entry.notes ? (
                <div className="detail-body-grid">
                    <div className="detail-main">
                        <div className="section-title">Notes</div>
                        <p className="detail-prose">{entry.notes}</p>
                    </div>
                    <div className="detail-side">{recipe}</div>
                </div>
            ) : (
                <div style={{ maxWidth: 520 }}>{recipe}</div>
            )}
        </div>
    )
}
