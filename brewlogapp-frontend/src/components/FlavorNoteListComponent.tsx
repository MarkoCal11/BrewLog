import { useState } from 'react'
import { useNavigate } from 'react-router'
import axios from 'axios'
import { useFlavorNotes } from '../hooks/useFlavorNotes'
import { createFlavorNote, deleteFlavorNote } from '../api/flavorNoteApi'
import type { FlavorNoteCreateRequest } from '../api/flavorNoteApi'
import type { FlavorNote } from '../types/FlavorNote'
import { ListToolbar } from './ListToolbar'
import { extractErrorMessage } from '../utils/errors'

const CATEGORIES = [
    'SWEET', 'SPICY', 'ROASTED', 'HERBAL', 'SOUR',
    'BITTER', 'FRUITY', 'NUTTY', 'FLORAL', 'CHOCOLATY',
]

const formatCategory = (c: string) => c.charAt(0) + c.slice(1).toLowerCase()

const emptyForm = (): FlavorNoteCreateRequest => ({
    name: '',
    category: '',
})

export function FlavorNoteListComponent() {
    const { data, error, loading, refetch, removeNote } = useFlavorNotes()
    const navigate = useNavigate()
    // GET returns globals + the caller's own personal notes. Personal notes are
    // the caller's to manage here; globals are admin-managed under /admin/flavor-notes.
    const canManage = (note: FlavorNote) => !note.global

    const [form, setForm] = useState<FlavorNoteCreateRequest>(emptyForm())
    const [saving, setSaving] = useState(false)
    const [saveError, setSaveError] = useState<string | null>(null)
    const [showForm, setShowForm] = useState(false)

    const [search, setSearch] = useState('')
    const [categoryFilter, setCategoryFilter] = useState('')
    const [scopeFilter, setScopeFilter] = useState<'all' | 'mine' | 'global'>('all')

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
        const { name, value } = e.target
        setForm(prev => ({ ...prev, [name]: value }))
    }

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        const name = form.name.trim()
        if (!name) {
            setSaveError('Name is required.')
            return
        }
        setSaving(true)
        setSaveError(null)
        try {
            await createFlavorNote({ ...form, name })
            setForm(emptyForm())
            setShowForm(false)
            refetch()
        } catch (err) {
            if (axios.isAxiosError(err) && err.response?.status === 409) {
                setSaveError('A flavor note with this name already exists.')
            } else {
                setSaveError(extractErrorMessage(err, 'Failed to save flavor note.'))
            }
        } finally {
            setSaving(false)
        }
    }

    const handleDelete = async (e: React.MouseEvent, note: { id: number; name: string }) => {
        e.stopPropagation()
        if (!window.confirm(`Delete "${note.name}"? This cannot be undone.`)) return
        try {
            removeNote(note.id)
            await deleteFlavorNote(note.id)
        } catch (err) {
            console.error('Failed to delete flavor note:', err)
            refetch()
        }
    }

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const term = search.trim().toLowerCase()
    const visibleNotes = (data ?? []).filter(note => {
        if (scopeFilter === 'mine' && note.global) return false
        if (scopeFilter === 'global' && !note.global) return false
        if (categoryFilter && note.category !== categoryFilter) return false
        if (term && !note.name.toLowerCase().includes(term)) return false
        return true
    })

    const hasFilters = search !== '' || categoryFilter !== '' || scopeFilter !== 'all'
    const clearFilters = () => {
        setSearch('')
        setCategoryFilter('')
        setScopeFilter('all')
    }

    return (
        <div className="stack-lg">
            <header className="row-between">
                <div>
                    <div className="page-eyebrow">BrewLog</div>
                    <h1 className="page-title">Flavors</h1>
                    <p className="page-subtitle">Your tasting vocabulary</p>
                </div>
                {!showForm && (
                    <button className="btn btn-outline" onClick={() => setShowForm(true)}>
                        + New
                    </button>
                )}
            </header>

            {showForm && (
                <form className="form" onSubmit={handleSubmit}>
                    <div className="field">
                        <label className="label">Name</label>
                        <input className="input" name="name" placeholder="e.g. Blueberry" value={form.name} onChange={handleChange} required />
                    </div>
                    <div className="field">
                        <label className="label">Category</label>
                        <select className="select" name="category" value={form.category} onChange={handleChange} required>
                            <option value="" disabled>Select a category</option>
                            {CATEGORIES.map(c => (
                                <option key={c} value={c}>{formatCategory(c)}</option>
                            ))}
                        </select>
                    </div>
                    <div className="field-row" style={{ marginTop: 4 }}>
                        <button type="button" className="btn btn-ghost btn-block" onClick={() => { setSaveError(null); setShowForm(false) }}>Cancel</button>
                        <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                            {saving ? 'Saving...' : 'Add Flavor Note'}
                        </button>
                    </div>
                    {saveError && <div className="error-inline">{saveError}</div>}
                </form>
            )}

            {(data ?? []).length === 0 ? (
                <div className="empty">No flavor notes yet.</div>
            ) : (
                <>
                    <ListToolbar
                        search={search}
                        onSearchChange={setSearch}
                        searchPlaceholder="Search flavor notes…"
                        showClear={hasFilters}
                        onClear={clearFilters}
                    >
                        <label className="control">
                            <span>Scope</span>
                            <select className="select" value={scopeFilter} onChange={e => setScopeFilter(e.target.value as 'all' | 'mine' | 'global')}>
                                <option value="all">All</option>
                                <option value="mine">Mine</option>
                                <option value="global">Global</option>
                            </select>
                        </label>
                        <label className="control">
                            <span>Category</span>
                            <select className="select" value={categoryFilter} onChange={e => setCategoryFilter(e.target.value)}>
                                <option value="">All</option>
                                {CATEGORIES.map(c => <option key={c} value={c}>{formatCategory(c)}</option>)}
                            </select>
                        </label>
                    </ListToolbar>

                    {visibleNotes.length === 0 ? (
                        <div className="empty">No flavor notes match your filters.</div>
                    ) : (
                        <>
                            <div className="list-count">{visibleNotes.length} of {(data ?? []).length} notes</div>
                            <ul className="list">
                                {visibleNotes.map(note => (
                                    <li
                                        key={note.id}
                                        className="list-item list-item-clickable"
                                        onClick={() => navigate(`/flavor-notes/${note.id}`)}
                                    >
                                        <div className="list-item-main">
                                            <div className="list-item-title">{note.name}</div>
                                            <div className="list-item-meta">{formatCategory(note.category)}</div>
                                        </div>
                                        {note.global && <span className="global-badge">Global</span>}
                                        {canManage(note) && (
                                            <button className="brew-del" onClick={(e) => handleDelete(e, note)}>Delete</button>
                                        )}
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
