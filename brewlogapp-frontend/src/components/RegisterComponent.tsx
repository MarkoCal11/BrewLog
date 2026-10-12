import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import axios from 'axios'
import { useAuth } from '../context/AuthContext'
import { extractErrorMessage } from '../utils/errors'

export function RegisterComponent() {
    const [firstName, setFirstName] = useState('')
    const [lastName, setLastName] = useState('')
    const [username, setUsername] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)
    const { register, isAuthenticated } = useAuth()
    const navigate = useNavigate()

    if (isAuthenticated) return <Navigate to="/" replace />

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')

        const trimmedEmail = email.trim()
        // Pragmatic check: something@something.something, no spaces. The backend's
        // @Email validation is the real gate; this is just a fast, friendly error.
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(trimmedEmail)) {
            setError('Please enter a valid email address')
            return
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match')
            return
        }

        if (password.length < 6) {
            setError('Password must be at least 6 characters')
            return
        }

        setLoading(true)
        try {
            await register({ firstName, lastName, username, email: trimmedEmail, password })
            navigate('/')
        } catch (err) {
            if (axios.isAxiosError(err) && err.response?.status === 409) {
                // The backend may reject a duplicate username OR email with 409;
                // surface its message if it sends one, else a fallback covering both.
                setError(extractErrorMessage(err, 'That username or email is already taken'))
            } else if (axios.isAxiosError(err) && !err.response) {
                setError('Cannot reach server. Is the backend running?')
            } else {
                setError(extractErrorMessage(err, 'Registration failed'))
            }
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className="auth-page">
            <div className="auth-card">
                <h1 className="auth-title">BrewLog</h1>
                <p className="auth-subtitle">Create your account</p>
                <form className="form" onSubmit={handleSubmit}>
                    <div className="field">
                        <label className="label">First Name</label>
                        <input
                            className="input"
                            type="text"
                            value={firstName}
                            onChange={e => setFirstName(e.target.value)}
                            autoComplete="given-name"
                            required
                        />
                    </div>
                    <div className="field">
                        <label className="label">Last Name</label>
                        <input
                            className="input"
                            type="text"
                            value={lastName}
                            onChange={e => setLastName(e.target.value)}
                            autoComplete="family-name"
                            required
                        />
                    </div>
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
                        <label className="label">Email</label>
                        <input
                            className="input"
                            type="email"
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            autoComplete="email"
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
                            autoComplete="new-password"
                            required
                        />
                    </div>
                    <div className="field">
                        <label className="label">Confirm Password</label>
                        <input
                            className="input"
                            type="password"
                            value={confirmPassword}
                            onChange={e => setConfirmPassword(e.target.value)}
                            autoComplete="new-password"
                            required
                        />
                    </div>
                    {error && <p className="error-inline">{error}</p>}
                    <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
                        {loading ? 'Creating account...' : 'Create Account'}
                    </button>
                </form>
                <p className="auth-footer">
                    Already have an account? <Link to="/login">Sign In</Link>
                </p>
            </div>
        </div>
    )
}
