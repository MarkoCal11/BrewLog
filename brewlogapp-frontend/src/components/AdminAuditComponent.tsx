import { useState } from 'react'
import { Link } from 'react-router'
import { useAuditLogs } from '../hooks/useAuditLogs'
import type { AuditAction, AuditLog } from '../types/AuditLog'

const ACTION_LABELS: Record<AuditAction, string> = {
    DEACTIVATE: 'Deactivate',
    REACTIVATE: 'Reactivate',
    ROLE_CHANGE: 'Role change',
}

const formatWhen = (iso: string) => new Date(iso).toLocaleString()

// De/reactivate store the active flag as "true"/"false"; show it in human terms.
const activeLabel = (v: string | null) =>
    v === 'true' ? 'Active' : v === 'false' ? 'Deactivated' : (v ?? '—')

function formatChange(log: AuditLog): string {
    if (log.action === 'ROLE_CHANGE') {
        return `${log.oldValue ?? '—'} → ${log.newValue ?? '—'}`
    }
    return `${activeLabel(log.oldValue)} → ${activeLabel(log.newValue)}`
}

export function AdminAuditComponent() {
    const [page, setPage] = useState(0)
    const { data, loading, error } = useAuditLogs(page)

    if (loading && !data) return <div className="empty">Loading...</div>
    if (error && !data) return <div className="error">Error: {error.message}</div>
    if (!data) return null

    const logs = data.content

    return (
        <div className="stack-lg">
            <Link to="/admin" className="back-link">Admin</Link>
            <header className="page-header">
                <h1 className="page-title">Audit Log</h1>
                <p className="page-subtitle">
                    {data.totalElements} admin {data.totalElements === 1 ? 'action' : 'actions'} on accounts, newest first.
                </p>
            </header>

            {logs.length === 0 ? (
                <div className="empty">No audit entries yet.</div>
            ) : (
                <>
                    <div className="card" style={{ overflowX: 'auto' }}>
                        <table className="stat-table">
                            <thead>
                                <tr>
                                    <th>When</th>
                                    <th>Actor</th>
                                    <th>Action</th>
                                    <th>Target</th>
                                    <th>Change</th>
                                </tr>
                            </thead>
                            <tbody>
                                {logs.map(log => (
                                    <tr key={log.id}>
                                        <td>{formatWhen(log.createdAt)}</td>
                                        <td>{log.actorUsername}</td>
                                        <td>{ACTION_LABELS[log.action] ?? log.action}</td>
                                        <td>{log.targetUsername ?? `#${log.targetUserId}`}</td>
                                        <td>{formatChange(log)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    <div className="row-between">
                        <button
                            className="btn btn-ghost"
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                            disabled={data.first || loading}
                        >
                            ← Newer
                        </button>
                        <span className="list-count">Page {data.number + 1} of {data.totalPages}</span>
                        <button
                            className="btn btn-ghost"
                            onClick={() => setPage(p => p + 1)}
                            disabled={data.last || loading}
                        >
                            Older →
                        </button>
                    </div>
                </>
            )}
        </div>
    )
}
