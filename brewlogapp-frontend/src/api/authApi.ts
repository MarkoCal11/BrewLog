import axios from 'axios'
import { Capacitor } from '@capacitor/core'
import type { AuthResponse } from '../types/Auth'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/auth`

// On the WEB the refresh token lives in an HttpOnly cookie, so credentials must
// ride along on every request. On NATIVE (Capacitor/iOS) the cookie isn't reliable
// in the webview, so the backend returns the refresh token in the response body
// (when we send `X-Client: mobile`) and we keep it in secure native storage.
const isMobile = Capacitor.isNativePlatform()
const REFRESH_KEY = 'refreshToken'

axios.defaults.withCredentials = true
if (isMobile) {
    // Signals the backend to return the rotating refresh token in the body.
    axios.defaults.headers.common['X-Client'] = 'mobile'
}

// The access token is kept in memory only — never localStorage/sessionStorage
// (XSS-exfiltratable). It is lost on reload and rehydrated via a silent refresh.
let accessToken: string | null = null

export const getAccessToken = (): string | null => accessToken

export const setAccessToken = (token: string | null): void => {
    accessToken = token
    if (token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
        delete axios.defaults.headers.common['Authorization']
    }
}

// Secure native storage for the refresh token (mobile only). Dynamically imported
// so the web bundle never pulls in the plugin.
async function storeRefreshToken(token: string | null): Promise<void> {
    const { Preferences } = await import('@capacitor/preferences')
    if (token) await Preferences.set({ key: REFRESH_KEY, value: token })
    else await Preferences.remove({ key: REFRESH_KEY })
}

async function readRefreshToken(): Promise<string | null> {
    const { Preferences } = await import('@capacitor/preferences')
    const { value } = await Preferences.get({ key: REFRESH_KEY })
    return value ?? null
}

export interface AuthRequest {
    username: string
    password: string
    rememberMe?: boolean
}

export interface RegisterRequest extends AuthRequest {
    email: string
    firstName?: string
    lastName?: string
}

export const loginWithOAuth = async (accessToken: string): Promise<AuthResponse> => {
    setAccessToken(accessToken)
    // Refresh cookie was set by the backend during the OAuth redirect (same as
    // regular login), so a normal cookie-based refresh gives us the user object.
    return refresh()
}

export const login = async (request: AuthRequest): Promise<AuthResponse> => {
    const { data } = await axios.post<AuthResponse>(`${API_URL}/login`, request)
    setAccessToken(data.accessToken)
    if (isMobile && data.refreshToken) await storeRefreshToken(data.refreshToken)
    return data
}

export const register = async (request: RegisterRequest): Promise<AuthResponse> => {
    const { data } = await axios.post<AuthResponse>(`${API_URL}/register`, request)
    setAccessToken(data.accessToken)
    if (isMobile && data.refreshToken) await storeRefreshToken(data.refreshToken)
    return data
}

// Logout must hit the endpoint so the server revokes the refresh token: on web JS
// can't delete the HttpOnly cookie; on mobile we must pass the stored token in the
// body so the server knows which one to kill, then wipe native storage.
export const logout = async (): Promise<void> => {
    try {
        if (isMobile) {
            const stored = await readRefreshToken()
            await axios.post(`${API_URL}/logout`, { refreshToken: stored })
        } else {
            await axios.post(`${API_URL}/logout`)
        }
    } finally {
        setAccessToken(null)
        if (isMobile) await storeRefreshToken(null)
    }
}

// Single-flight refresh. Refresh tokens ROTATE and the backend does reuse
// detection: presenting the same (already-consumed) token twice is treated as
// theft and nukes every session for the user. So all concurrent callers must
// await the SAME in-flight promise rather than each firing their own /refresh,
// and every refresh must overwrite the stored token with the rotated one.
// Module-level scope makes this survive React StrictMode's double-invoked effects.
let refreshPromise: Promise<AuthResponse> | null = null

export const refresh = (): Promise<AuthResponse> => {
    if (!refreshPromise) {
        refreshPromise = doRefresh().finally(() => {
            refreshPromise = null
        })
    }
    return refreshPromise
}

async function doRefresh(): Promise<AuthResponse> {
    if (isMobile) {
        const stored = await readRefreshToken()
        // No stored token = no session to rehydrate (e.g. fresh install / boot).
        // Reject so callers fall through to the unauthenticated state.
        if (!stored) throw new Error('No stored refresh token')
        const { data } = await axios.post<AuthResponse>(`${API_URL}/refresh`, {
            refreshToken: stored,
        })
        setAccessToken(data.accessToken)
        // Rotation: persist the NEW token; sending the old one again triggers
        // reuse-detection and forces a re-login.
        if (data.refreshToken) await storeRefreshToken(data.refreshToken)
        return data
    }
    // Web: the cookie is sent automatically — no body.
    const { data } = await axios.post<AuthResponse>(`${API_URL}/refresh`)
    setAccessToken(data.accessToken)
    return data
}

// Proactive refresh. The access token is short-lived, and the response-side 401
// interceptor (AuthContext) only recovers a token AFTER a 401 comes back. That
// works for normal requests but NOT for uploads: a 401 arriving while the request
// body is still uploading makes the browser reset the connection into an opaque
// "network error" with no response object to react to. So we top the token up
// BEFORE sending whenever it's expired or about to expire — this matters most for
// a slow-to-start photo upload (e.g. a Google Drive file that takes seconds to
// stream in), which then leaves with a full-lifetime token. Single-flight
// `refresh()` de-dupes concurrent callers.
const EXPIRY_SKEW_MS = 10_000

function accessTokenExpiresAt(token: string): number | null {
    try {
        const payload = token.split('.')[1]
        const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
        return typeof json.exp === 'number' ? json.exp * 1000 : null
    } catch {
        return null
    }
}

axios.interceptors.request.use(async config => {
    // Auth endpoints manage tokens themselves; refreshing here would recurse.
    if (config.url?.includes('/api/auth/')) return config

    const token = getAccessToken()
    if (!token) return config

    const expiresAt = accessTokenExpiresAt(token)
    if (expiresAt === null) return config                       // can't tell — send as-is
    if (expiresAt - Date.now() > EXPIRY_SKEW_MS) return config  // still comfortably valid

    // Expired or within the skew window: refresh first, then send with the new token.
    try {
        const data = await refresh()
        config.headers.Authorization = `Bearer ${data.accessToken}`
    } catch {
        // No live session to refresh — let the request go out and the response-side
        // 401 handler clear auth and route to login.
    }
    return config
})
