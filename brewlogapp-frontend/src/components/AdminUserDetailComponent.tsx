import { useState, useEffect } from 'react'
import { Link, useParams } from 'react-router'
import { useAuditLogs } from '../hooks/useAuditLogs'
import { useAuth } from '../context/AuthContext'
import { getUser, getUserStats, deactivateUser, reactivateUser, updateUserRole } from '../api/adminUserApi'
import { extractErrorMessage } from '../utils/errors'
import type { AdminUser, AdminUserStats } from '../types/Auth'
import type { AuditAction, AuditLog } from '../types/AuditLog'

// --- Tier 3 audit-table formatting (kept in sync with AdminAuditComponent) ---
const ACTION_LABELS: Record<AuditAction, string> = {
    DEACTIVATE: 'Deactivate',
    REACTIVATE: 'Reactivate',
    ROLE_CHANGE: 'Role change',
}
const formatWhen = (iso: string) => new Date(iso).toLocaleString()
const activeLabel = (v: string | null) =>
    v === 'true' ? 'Active' : v === 'false' ? 'Deactivated' : (v ?? '—')
const formatChange = (log: AuditLog) =>
    log.action === 'ROLE_CHANGE'
        ? `${log.oldValue ?? '—'} → ${log.newValue ?? '—'}`
        : `${activeLabel(log.oldValue)} → ${activeLabel(log.newValue)}`

export function AdminUserDetailComponent() {
    const { id } = useParams<{ id: string }>()
    const userId = Number(id)
    const { user: currentUser } = useAuth()

    // Tier 1 + Tier 2: the account and its aggregate counts, fetched directly so
    // the page deep-links/refreshes without pulling the whole user/bean/brew lists.
    const [u, setU] = useState<AdminUser | null>(null)
    const [stats, setStats] = useState<AdminUserStats | null>(null)
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)

    const [busy, setBusy] = useState(false)
    const [actionError, setActionError] = useState<string | null>(null)

    // Tier 3: admin actions on THIS account, filtered server-side by targetUserId.
    const { data: auditPage } = useAuditLogs(0, 50, userId)

    useEffect(() => {
        let cancelled = false
        setLoading(true)
        Promise.all([getUser(userId), getUserStats(userId)])
            .then(([user, s]) => {
                if (cancelled) return
                setU(user)
                setStats(s)
                setLoading(false)
            })
            .catch(err => {
                if (cancelled) return
                setError(err)
                setLoading(false)
            })
        return () => { cancelled = true }
    }, [userId])

    if (loading) return <div className="empty">Loading...</div>
    if (error) return <div className="error">Error: {error.message}</div>
    if (!u) return (
        <div className="stack-lg">
            <Link to="/admin/users" className="back-link">Users</Link>
            <div className="empty">User not found.</div>
        </div>
    )

    // Self-guards are enforceable client-side; the last-active-admin guard needs the
    // full roster, so it's left to the server, which rejects and we surface the error.
    const isSelf = currentUser?.id === u.id
    const selfDeactivateReason = isSelf ? "You can't deactivate your own account" : null
    const selfRoleReason = isSelf ? "You can't change your own role" : null

    const run = (action: () => Promise<AdminUser>) => async () => {
        setBusy(true)
        setActionError(null)
        try {
            setU(await action())
        } catch (err) {
            setActionError(extractErrorMessage(err))
        } finally {
            setBusy(false)
        }
    }

    const handleDeactivate = () => {
        if (!window.confirm(`Deactivate "${u.username}"? They won't be able to sign in until reactivated.`)) return
        return run(() => deactivateUser(u.id))()
    }
    const handleReactivate = () => {
        if (!window.confirm(`Reactivate "${u.username}"? They'll be able to sign in again.`)) return
        return run(() => reactivateUser(u.id))()
    }
    const handlePromote = () => {
        if (!window.confirm(`Make "${u.username}" an admin? They'll get full access to every user's data.`)) return
        return run(() => updateUserRole(u.id, 'ADMIN'))()
    }
    const handleDemote = () => {
        if (!window.confirm(`Remove admin access from "${u.username}"?`)) return
        return run(() => updateUserRole(u.id, 'USER'))()
    }

    const joined = u.createdAt ? new Date(u.createdAt).toLocaleDateString() : null
    const userLogs = auditPage?.content ?? []

    return (
        <div className="stack-lg">
            <Link to="/admin/users" className="back-link">Users</Link>

            <header className="row-between">
                <div>
                    <h1 className="page-title">
                        {u.username}
                        {u.role === 'ADMIN' && <span className="role-badge">Admin</span>}
                    </h1>
                    <p className="page-subtitle">{u.active ? 'Active' : 'Deactivated'}</p>
                </div>
                <div className="row-actions">
                    {u.role === 'ADMIN' ? (
                        <button
                            className="btn btn-ghost"
                            onClick={handleDemote}
                            disabled={busy || selfRoleReason != null}
                            title={selfRoleReason ?? undefined}
                        >
                            Make user
                        </button>
                    ) : (
                        <button className="btn btn-ghost" onClick={handlePromote} disabled={busy}>
                            Make admin
                        </button>
                    )}
                    {u.active ? (
                        <button
                            className="btn btn-danger"
                            onClick={handleDeactivate}
                            disabled={busy || selfDeactivateReason != null}
                            title={selfDeactivateReason ?? undefined}
                        >
                            {busy ? '…' : 'Deactivate'}
                        </button>
                    ) : (
                        <button className="btn btn-success" onClick={handleReactivate} disabled={busy}>
                            {busy ? '…' : 'Reactivate'}
                        </button>
                    )}
                </div>
            </header>
            {actionError && <div className="error-inline">{actionError}</div>}

            {/* TIER 1 — administrative facts about the account */}
            <div>
                <div className="section-title">Account</div>
                <div className="detail-list">
                    <div className="detail-row">
                        <span className="detail-label">Status</span>
                        <span className="detail-value">{u.active ? 'Active' : 'Deactivated'}</span>
                    </div>
                    <div className="detail-row">
                        <span className="detail-label">Role</span>
                        <span className="detail-value">{u.role === 'ADMIN' ? 'Admin' : 'User'}</span>
                    </div>
                    {u.email && (
                        <div className="detail-row">
                            <span className="detail-label">Email</span>
                            <span className="detail-value">{u.email}</span>
                        </div>
                    )}
                    {joined && (
                        <div className="detail-row">
                            <span className="detail-label">Joined</span>
                            <span className="detail-value">{joined}</span>
                        </div>
                    )}
                </div>
            </div>

            {/* TIER 2 — aggregate activity, counts only (no drill-down) */}
            {stats && (
                <div className="stat-row">
                    <div className="stat">
                        <div className="stat-label">Beans</div>
                        <div className="stat-value">{stats.beanCount}</div>
                    </div>
                    <div className="stat">
                        <div className="stat-label">Brews</div>
                        <div className="stat-value">{stats.brewCount}</div>
                    </div>
                    <div className="stat">
                        <div className="stat-label">Cafés</div>
                        <div className="stat-value">{stats.cafeCount}</div>
                    </div>
                </div>
            )}

            {/* TIER 3 — admin actions taken on this account */}
            <div>
                <div className="section-title">Admin actions on this account</div>
                {userLogs.length === 0 ? (
                    <div className="empty">No admin actions recorded.</div>
                ) : (
                    <div className="card" style={{ overflowX: 'auto' }}>
                        <table className="stat-table">
                            <thead>
                                <tr>
                                    <th>When</th>
                                    <th>Actor</th>
                                    <th>Action</th>
                                    <th>Change</th>
                                </tr>
                            </thead>
                            <tbody>
                                {userLogs.map(log => (
                                    <tr key={log.id}>
                                        <td>{formatWhen(log.createdAt)}</td>
                                        <td>{log.actorUsername}</td>
                                        <td>{ACTION_LABELS[log.action] ?? log.action}</td>
                                        <td>{formatChange(log)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            {/* Deliberately absent: the user's own beans / brews / tasting notes.
                Admin oversight stops at administrative facts + aggregates; per-user
                content stays owner-only (see project_auth_plan ownership rule). */}
        </div>
    )
}
