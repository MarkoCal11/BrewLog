import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import axios from 'axios'
import { useAdminFlavorNotes } from '../hooks/useAdminFlavorNotes'
import { createGlobalFlavorNote, deleteGlobalFlavorNote } from '../api/flavorNoteApi'
import type { FlavorNote } from '../types/FlavorNote'
import { extractErrorMessage } from '../utils/errors'

const CATEGORIES = [
    'SWEET', 'SPICY', 'ROASTED', 'HERBAL', 'SOUR',
    'BITTER', 'FRUITY', 'NUTTY', 'FLORAL', 'CHOCOLATY',
]

const formatCategory = (c: string) => c.charAt(0) + c.slice(1).toLowerCase()

type NoteForm = { name: string; category: string }

const emptyForm = (): NoteForm => ({ name: '', category: '' })

export function AdminFlavorNotesComponent() {
    const { data, error, loading, refetch } = useAdminFlavorNotes()
    const navigate = useNavigate()

    const [form, setForm] = useState<NoteForm>(emptyForm())
    const [showForm, setShowForm] = useState(false)
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [actionError, setActionError] = useState<string | null>(null)

    const dupMessage = (err: unknown) =>
        axios.isAxiosError(err) && err.response?.status === 409
            ? 'A global flavor note with this name already exists.'
            : extractErrorMessage(err)

    const handleCreate = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        const name = form.name.trim()
        if (!name) {
            setSaveError('Name is required.')
            return
        }
        setSaving(true)
        setSaveError(null)
        try {
            await createGlobalFlavorNote({ ...form, name })
            setForm(emptyForm())
            setShowForm(false)
            refetch()
        } catch (err) {
            setSaveError(dupMessage(err))
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (e: React.MouseEvent, note: FlavorNote) => {
        e.stopPropagation()
        if (!window.confirm(`Delete the global note "${note.name}"? It will be removed from every bean that uses it.`)) return
        setActionError(null)
        try {
            await deleteGlobalFlavorNote(note.id)
            refetch()
        } catch (err) {
            setActionError(extractErrorMessage(err))
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="row-between">
                <div>
                    <h1 className="page-title">Global Flavors</h1>
                    <p className="page-subtitle">{(data ?? []).length} shared notes, visible to every user.</p>
                </div>
                {!showForm && (
                    <button className="btn btn-ghost" onClick={() => setShowForm(true)}>
                        + New
                    </button>
                )}
            </header>

            {showForm && (
                <form className="form" onSubmit={handleCreate}>
                    <div className="field">
                        <label className="label">Name</label>
                        <input className="input" name="name" placeholder="e.g. Blueberry" value={form.name} onChange={e => setForm(p => ({ ...p, name: e.target.value }))} required />
                    </div>
                    <div className="field">
                        <label className="label">Category</label>
                        <select className="select" name="category" value={form.category} onChange={e => setForm(p => ({ ...p, category: e.target.value }))} required>
                            <option value="" disabled>Select a category</option>
                            {CATEGORIES.map(c => <option key={c} value={c}>{formatCategory(c)}</option>)}
                        </select>
                    </div>
                    <div className="field-row" style={{ marginTop: 4 }}>
                        <button type="button" className="btn btn-ghost btn-block" onClick={() => { setSaveError(null); setShowForm(false) }}>Cancel</button>
                        <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                            {saving ? 'Saving...' : 'Add Global Note'}
                        </button>
                    </div>
                    {saveError && <div className="error-inline">{saveError}</div>}
                </form>
            )}

            {actionError && <div className="error-inline">{actionError}</div>}

            {(data ?? []).length === 0 ? (
                <div className="empty">No global flavor notes yet.</div>
            ) : (
                <ul className="list">
                    {(data ?? []).map(note => (
                        <li
                            key={note.id}
                            className="list-item list-item-clickable"
                            onClick={() => navigate(`/admin/flavor-notes/${note.id}`)}
                        >
                            <div className="list-item-main">
                                <div className="list-item-title">{note.name}</div>
                                <div className="list-item-meta">{formatCategory(note.category)}</div>
                            </div>
                            <span className="global-badge">Global</span>
                            <button className="brew-del" onClick={(e) => handleDelete(e, note)}>Delete</button>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}
