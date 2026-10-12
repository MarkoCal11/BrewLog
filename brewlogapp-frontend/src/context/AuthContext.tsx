import { createContext, useContext, useState, useEffect, type ReactNode } from 'react'
import type { User } from '../types/Auth'
import {
    login as apiLogin,
    register as apiRegister,
    refresh as apiRefresh,
    logout as apiLogout,
    loginWithOAuth as apiLoginWithOAuth,
    setAccessToken,
    type AuthRequest,
    type RegisterRequest,
} from '../api/authApi'
import axios from 'axios'

interface AuthContextType {
    user: User | null
    isAuthenticated: boolean
    loading: boolean
    login: (request: AuthRequest) => Promise<void>
    register: (request: RegisterRequest) => Promise<void>
    logout: () => Promise<void>
    loginWithOAuth: (accessToken: string) => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    // The access token is memory-only, so on every reload we don't yet know if
    // there's a live session. `loading` keeps the router from bouncing to
    // /login until the silent boot refresh resolves.
    const [loading, setLoading] = useState(true)

    // Refresh-and-retry interceptor. 401 = not authenticated → refresh once and
    // retry. 403 (authenticated but forbidden) must pass straight through;
    // refreshing won't help. Refresh itself is single-flight in authApi, so N
    // concurrent 401s all await the same /refresh.
    useEffect(() => {
        const interceptor = axios.interceptors.response.use(
            response => response,
            async error => {
                const originalRequest = error.config
                const isRefreshCall = originalRequest?.url?.includes('/api/auth/refresh')

                if (
                    error.response?.status !== 401 ||
                    !originalRequest ||
                    originalRequest._retry ||
                    isRefreshCall
                ) {
                    return Promise.reject(error)
                }

                originalRequest._retry = true

                try {
                    const data = await apiRefresh()
                    setUser(data.user)
                    originalRequest.headers['Authorization'] = `Bearer ${data.accessToken}`
                    return axios(originalRequest)
                } catch (refreshError) {
                    setAccessToken(null)
                    setUser(null)
                    return Promise.reject(refreshError)
                }
            }
        )

        return () => axios.interceptors.response.eject(interceptor)
    }, [])

    // On boot the refresh cookie persists even though the access token is gone,
    // so attempt one silent refresh to rehydrate the session.
    useEffect(() => {
        let active = true
        apiRefresh()
            .then(data => {
                if (active) setUser(data.user)
            })
            .catch(() => {
                if (active) setUser(null)
            })
            .finally(() => {
                if (active) setLoading(false)
            })
        return () => {
            active = false
        }
    }, [])

    const login = async (request: AuthRequest) => {
        const data = await apiLogin(request)
        setUser(data.user)
    }

    const register = async (request: RegisterRequest) => {
        const data = await apiRegister(request)
        setUser(data.user)
    }

    const logout = async () => {
        await apiLogout()
        setUser(null)
    }

    const loginWithOAuth = async (accessToken: string) => {
        const data = await apiLoginWithOAuth(accessToken)
        setUser(data.user)
    }

    return (
        <AuthContext.Provider
            value={{ user, isAuthenticated: !!user, loading, login, register, logout, loginWithOAuth }}
        >
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    const context = useContext(AuthContext)
    if (!context) throw new Error('useAuth must be used within AuthProvider')
    return context
}
