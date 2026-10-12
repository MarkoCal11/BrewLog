import { Link } from 'react-router'

export function AdminComponent() {
    return (
        <div className="stack-lg">
            <header className="page-header">
                <div className="page-eyebrow">BrewLog</div>
                <h1 className="page-title">Admin</h1>
                <p className="page-subtitle">Global view and management across all users.</p>
            </header>

            <div>
                <div className="section-title">Global data</div>
                <ul className="list admin-grid">
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/beans" className="list-item-main">
                            <div className="list-item-title">All Beans</div>
                            <div className="list-item-meta">Every coffee bean logged by any user</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/brews" className="list-item-main">
                            <div className="list-item-title">All Brews</div>
                            <div className="list-item-meta">Every brew entry logged by any user</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/cafes" className="list-item-main">
                            <div className="list-item-title">All Cafés</div>
                            <div className="list-item-meta">Every café pinned by any user, on one map</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/flavor-notes" className="list-item-main">
                            <div className="list-item-title">Global Flavors</div>
                            <div className="list-item-meta">Create and manage the shared flavor-note vocabulary</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/analytics" className="list-item-main">
                            <div className="list-item-title">Global Analytics</div>
                            <div className="list-item-meta">Stats across all beans and brews from every user</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/reports" className="list-item-main">
                            <div className="list-item-title">Global Reports</div>
                            <div className="list-item-meta">Periodic brew reports aggregated from every user</div>
                        </Link>
                    </li>
                </ul>
            </div>

            <div>
                <div className="section-title">Administration</div>
                <ul className="list admin-grid">
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/users" className="list-item-main">
                            <div className="list-item-title">Users</div>
                            <div className="list-item-meta">List accounts; deactivate, reactivate, change role</div>
                        </Link>
                    </li>
                    <li className="list-item list-item-clickable">
                        <Link to="/admin/audit" className="list-item-main">
                            <div className="list-item-title">Audit Log</div>
                            <div className="list-item-meta">Read-only history of admin actions on accounts</div>
                        </Link>
                    </li>
                </ul>
            </div>
        </div>
    )
}
