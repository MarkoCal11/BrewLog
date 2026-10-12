export type Role = 'USER' | 'ADMIN'

export interface User {
    id: number
    username: string
    displayName?: string
    firstName?: string
    lastName?: string
    role: Role
}

export interface AuthResponse {
    accessToken: string
    user: User
    // Mobile (Capacitor) only: the backend returns the rotating refresh token in
    // the body when the client sends `X-Client: mobile`. Absent on web (the web
    // build keeps the token in an HttpOnly cookie).
    refreshToken?: string
}

/** User as seen in the admin user-management list (richer than the auth User). */
export interface AdminUser {
    id: number
    username: string
    role: Role
    active: boolean
    // Optional until the backend admin DTO exposes them.
    email?: string | null
    createdAt?: string | null
}

/** Aggregate activity counts for one user (GET /api/admin/users/:id/stats). */
export interface AdminUserStats {
    beanCount: number
    brewCount: number
    cafeCount: number
}
