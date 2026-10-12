import { useState, useEffect } from 'react'
import { Link, useParams } from 'react-router'
import axios from 'axios'
import { getFlavorNote, updateFlavorNote } from '../api/flavorNoteApi'
import type { FlavorNote } from '../types/FlavorNote'
import { extractErrorMessage } from '../utils/errors'

const CATEGORIES = [
    'SWEET', 'SPICY', 'ROASTED', 'HERBAL', 'SOUR',
    'BITTER', 'FRUITY', 'NUTTY', 'FLORAL', 'CHOCOLATY',
]

type EditForm = {
    name: string
    category: string
}

export function FlavorNoteDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const [note, setNote] = useState<FlavorNote | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)
    const [editing, setEditing] = useState(false)
    const [editForm, setEditForm] = useState<EditForm | null>(null)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)

    useEffect(() => {
        getFlavorNote(Number(id))
            .then(setNote)
            .catch(err => setError(err))
            .finally(() => setLoading(false))
    }, [id])

    const startEditing = () => {
        if (!note) return
        setEditForm({ name: note.name, category: note.category })
        setEditing(true)
    }

    const handleEditChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target
        setEditForm(prev => prev ? { ...prev, [name]: value } : prev)
    }

    const handleEditSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!editForm) return
        const name = editForm.name.trim()
        if (!name) {
            setSaveError('Name is required.')
            return
        }
        if (!window.confirm('Save changes to this flavor note?')) return
        setSaving(true)
        setSaveError(null)
        try {
            const updated = await updateFlavorNote(Number(id), { ...editForm, name })
            setNote(updated)
            setEditing(false)
        } catch (err) {
            if (axios.isAxiosError(err) && err.response?.status === 409) {
                setSaveError('A flavor note with this name already exists.')
            } else {
                setSaveError(extractErrorMessage(err, 'Failed to save changes.'))
            }
        } finally {
            setSaving(false)
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!note) return <div className="empty">Flavor note not found.</div>

    // Globals are admin-managed under /admin/flavor-notes; personal notes (the caller's own) are editable here.
    const canManage = !note.global

    return (
        <div className="stack-lg">
            <Link to="/flavor-notes" className="back-link">Flavors</Link>

            {editing && editForm ? (
                <>
                    <h1 className="page-title">Edit Flavor Note</h1>
                    <form className="form" onSubmit={handleEditSubmit}>
                        <div className="field">
                            <label className="label">Name</label>
                            <input className="input" name="name" value={editForm.name} onChange={handleEditChange} required />
                        </div>
                        <div className="field">
                            <label className="label">Category</label>
                            <select className="select" name="category" value={editForm.category} onChange={handleEditChange} required>
                                <option value="" disabled>Select a category</option>
                                {CATEGORIES.map(c => (
                                    <option key={c} value={c}>{c.charAt(0) + c.slice(1).toLowerCase()}</option>
                                ))}
                            </select>
                        </div>
                        <div className="field-row" style={{ marginTop: 4 }}>
                            <button type="button" className="btn btn-ghost btn-block" onClick={() => setEditing(false)}>Cancel</button>
                            <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                                {saving ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                        {saveError && <div className="error-inline">{saveError}</div>}
                    </form>
                </>
            ) : (
                <>
                    <header className="row-between">
                        <div>
                            <h1 className="page-title">{note.name}</h1>
                            <p className="page-subtitle">{note.category.charAt(0) + note.category.slice(1).toLowerCase()}</p>
                        </div>
                        <div className="row-actions">
                            {note.global && <span className="global-badge">Global</span>}
                            {canManage && <button className="btn btn-ghost" onClick={startEditing}>Edit</button>}
                        </div>
                    </header>

                    <div className="detail-list">
                        <div className="detail-row">
                            <span className="detail-label">Category</span>
                            <span className="detail-value">{note.category.charAt(0) + note.category.slice(1).toLowerCase()}</span>
                        </div>
                    </div>
                </>
            )}
        </div>
    )
}
