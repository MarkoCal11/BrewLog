import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { Capacitor } from '@capacitor/core'
import { useAuth } from '../context/AuthContext'
import { BACKEND_URL } from '../api/config'

export function LoginComponent() {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [rememberMe, setRememberMe] = useState(false)
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const { login, isAuthenticated } = useAuth()
    const navigate = useNavigate()

    if (isAuthenticated) return <Navigate to="/" replace />

    const handleGoogleLogin = () => {
        const oauthUrl = `${BACKEND_URL}/oauth2/authorization/google`
        if (Capacitor.isNativePlatform()) {
            import('@capacitor/browser').then(({ Browser }) =>
                Browser.open({ url: oauthUrl })
            )
        } else {
            window.location.href = oauthUrl
        }
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            await login({ username, password, rememberMe })
            navigate('/')
        } catch {
            setError('Invalid username or password')
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1 className="auth-title">BrewLog</h1>
                <p className="auth-subtitle">Sign in to your account</p>
                <form className="form" onSubmit={handleSubmit}>
                    <div className="field">
                        <label className="label">Username</label>
                        <input
                            className="input"
                            type="text"
                            value={username}
                            onChange={e => setUsername(e.target.value)}
                            autoComplete="username"
                            required
                        />
                    </div>
                    <div className="field">
                        <label className="label">Password</label>
                        <input
                            className="input"
                            type="password"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            autoComplete="current-password"
                            required
                        />
                    </div>
                    <label className="remember-me">
                        <input
                            type="checkbox"
                            checked={rememberMe}
                            onChange={e => setRememberMe(e.target.checked)}
                        />
                        Remember me
                    </label>
                    {error && <p className="error-inline">{error}</p>}
                    <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
                        {loading ? 'Signing in...' : 'Sign In'}
                    </button>
                </form>
                <div className="auth-divider">or</div>
                <button className="btn btn-outline btn-block" type="button" onClick={handleGoogleLogin}>
                    Login with Google
                </button>
                <p className="auth-footer">
                    Don't have an account? <Link to="/register">Sign Up</Link>
                </p>
            </div>
        </div>
    )
}
