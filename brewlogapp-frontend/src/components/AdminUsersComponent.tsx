import { useState } from 'react'
import { Link } from 'react-router'
import { useAdminUsers } from '../hooks/useAdminUsers'
import { useAuth } from '../context/AuthContext'
import { deactivateUser, reactivateUser, updateUserRole } from '../api/adminUserApi'
import { extractErrorMessage } from '../utils/errors'
import type { AdminUser } from '../types/Auth'

export function AdminUsersComponent() {
    const { data, error, loading, replaceUser } = useAdminUsers()
    const { user: currentUser } = useAuth()
    const [busyId, setBusyId] = useState<number | null>(null)
    const [actionError, setActionError] = useState<string | null>(null)

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>

    const users = data ?? []
    // Mirror of the server-side guard: never let the last active admin be locked out.
    const activeAdminCount = users.filter(u => u.active && u.role === 'ADMIN').length

    // Order: active accounts first, admins before regular users, then A–Z by name —
    // so the accounts you're most likely to act on sit at the top.
    const sortedUsers = [...users].sort((a, b) => {
        if (a.active !== b.active) return a.active ? -1 : 1
        if (a.role !== b.role) return a.role === 'ADMIN' ? -1 : 1
        return a.username.localeCompare(b.username)
    })

    const formatJoined = (iso?: string | null) => (iso ? new Date(iso).toLocaleDateString() : null)

    const handleDeactivate = async (u: AdminUser) => {
        if (!window.confirm(`Deactivate "${u.username}"? They won't be able to sign in until reactivated.`)) return
        setBusyId(u.id)
        setActionError(null)
        try {
            replaceUser(await deactivateUser(u.id))
        } catch (err) {
            setActionError(extractErrorMessage(err))
        } finally {
            setBusyId(null)
        }
    }

    const handleReactivate = async (u: AdminUser) => {
        if (!window.confirm(`Reactivate "${u.username}"? They'll be able to sign in again.`)) return
        setBusyId(u.id)
        setActionError(null)
        try {
            replaceUser(await reactivateUser(u.id))
        } catch (err) {
            setActionError(extractErrorMessage(err))
        } finally {
            setBusyId(null)
        }
    }

    const handlePromote = async (u: AdminUser) => {
        if (!window.confirm(`Make "${u.username}" an admin? They'll get full access to every user's data.`)) return
        setBusyId(u.id)
        setActionError(null)
        try {
            replaceUser(await updateUserRole(u.id, 'ADMIN'))
        } catch (err) {
            setActionError(extractErrorMessage(err))
        } finally {
            setBusyId(null)
        }
    }

    const handleDemote = async (u: AdminUser) => {
        if (!window.confirm(`Remove admin access from "${u.username}"?`)) return
        setBusyId(u.id)
        setActionError(null)
        try {
            replaceUser(await updateUserRole(u.id, 'USER'))
        } catch (err) {
            setActionError(extractErrorMessage(err))
        } finally {
            setBusyId(null)
        }
    }

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">Users</h1>
                <p className="page-subtitle">{users.length} registered {users.length === 1 ? 'account' : 'accounts'}.</p>
            </header>

            {actionError && <div className="error-inline">{actionError}</div>}

            {users.length === 0 ? (
                <div className="empty">No users found.</div>
            ) : (
                <ul className="list">
                    {sortedUsers.map(u => {
                        const isSelf = currentUser?.id === u.id
                        const isLastAdmin = u.role === 'ADMIN' && u.active && activeAdminCount <= 1
                        const blockReason = isSelf
                            ? "You can't deactivate your own account"
                            : isLastAdmin
                                ? "Can't deactivate the last active admin"
                                : null
                        const demoteReason = isSelf
                            ? "You can't change your own role"
                            : isLastAdmin
                                ? "Can't demote the last active admin"
                                : null
                        const joined = formatJoined(u.createdAt)
                        return (
                            <li key={u.id} className={`list-item user-row${u.active ? '' : ' user-deactivated'}`}>
                                <div className="list-item-main">
                                    <div className="list-item-title">
                                        <Link to={`/admin/users/${u.id}`}>{u.username}</Link>
                                        {u.role === 'ADMIN' && <span className="role-badge">Admin</span>}
                                    </div>
                                    <div className="list-item-meta">
                                        {u.active ? 'Active' : 'Deactivated'}
                                        {u.email && ` · ${u.email}`}
                                        {joined && ` · joined ${joined}`}
                                    </div>
                                </div>
                                <div className="row-actions">
                                    {u.role === 'ADMIN' ? (
                                        <button
                                            className="btn btn-ghost"
                                            onClick={() => handleDemote(u)}
                                            disabled={busyId === u.id || demoteReason != null}
                                            title={demoteReason ?? undefined}
                                        >
                                            Make user
                                        </button>
                                    ) : (
                                        <button
                                            className="btn btn-ghost"
                                            onClick={() => handlePromote(u)}
                                            disabled={busyId === u.id}
                                        >
                                            Make admin
                                        </button>
                                    )}
                                    {u.active ? (
                                        <button
                                            className="btn btn-danger"
                                            onClick={() => handleDeactivate(u)}
                                            disabled={busyId === u.id || blockReason != null}
                                            title={blockReason ?? undefined}
                                        >
                                            {busyId === u.id ? '…' : 'Deactivate'}
                                        </button>
                                    ) : (
                                        <button
                                            className="btn btn-success"
                                            onClick={() => handleReactivate(u)}
                                            disabled={busyId === u.id}
                                        >
                                            {busyId === u.id ? '…' : 'Reactivate'}
                                        </button>
                                    )}
                                </div>
                            </li>
                        )
                    })}
                </ul>
            )}
        </div>
    )
}
