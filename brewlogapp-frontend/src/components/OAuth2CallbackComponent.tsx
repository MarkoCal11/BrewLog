import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router'
import { useAuth } from '../context/AuthContext'

export function OAuth2CallbackComponent() {
    const navigate = useNavigate()
    const { loginWithOAuth } = useAuth()
    // Read at render time so StrictMode's double-effect doesn't lose them after replaceState
    const params = useRef(new URLSearchParams(window.location.search))
    const processed = useRef(false)

    useEffect(() => {
        if (processed.current) return
        processed.current = true

        const token = params.current.get('token')

        if (!token) {
            navigate('/login', { replace: true })
            return
        }

        window.history.replaceState({}, '', window.location.pathname)

        loginWithOAuth(token)
            .then(() => navigate('/', { replace: true }))
            .catch(() => navigate('/login', { replace: true }))
    }, [])

    return <div className="empty">Signing in…</div>
}
