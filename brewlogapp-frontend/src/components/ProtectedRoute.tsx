import { Navigate, Outlet } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function ProtectedRoute() {
    const { isAuthenticated, loading } = useAuth()

    // Wait for the silent boot refresh before deciding — otherwise every reload
    // flashes /login before the session rehydrates.
    if (loading) {
        return null
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />
    }

    return <Outlet />
}
