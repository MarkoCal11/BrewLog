import { NavLink, Outlet } from 'react-router'
import { ThemeToggle } from './ThemeToggle'
import { useAuth } from '../context/AuthContext'

export function Layout() {
    const { user, logout } = useAuth()

    const handleLogout = () => {
        if (window.confirm('Log out of BrewLog?')) {
            logout()
        }
    }

    return (
        <div className="app-layout">
            <nav className="app-nav">
                <div className="app-nav-inner">
                    {/* Brand doubles as the Home link. `end` so it only marks active on
                        exactly "/", not every route (NavLink prefix-matches otherwise). */}
                    <NavLink to="/" end className="app-nav-brand">Home</NavLink>
                    <NavLink to="/beans">Beans</NavLink>
                    <NavLink to="/brews">Brews</NavLink>
                    <NavLink to="/flavor-notes">Flavors</NavLink>
                    <NavLink to="/cafes">Cafés</NavLink>
                    <NavLink to="/analytics">Analytics</NavLink>
                    <NavLink to="/reports">Reports</NavLink>
                    {user?.role === 'ADMIN' && <NavLink to="/admin">Admin</NavLink>}
                    <ThemeToggle />
                    {user && (
                        <>
                            <span className="nav-username">{user.username}</span>
                            <button className="btn-logout" onClick={handleLogout}>
                                Log out
                            </button>
                        </>
                    )}
                </div>
            </nav>
            <main className="container">
                <Outlet />
            </main>
        </div>
    )
}
