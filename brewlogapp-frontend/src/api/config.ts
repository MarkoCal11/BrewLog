// Backend origin. Centralized here so photo URLs (stored as relative paths like
// "/uploads/beans/12.jpg") resolve correctly across localhost, a Mac LAN IP, or a
// hosted backend — change one env var, not stored data. (The per-resource API_URL
// constants still hardcode the host today; this is the seed for that later refactor.)
const env = import.meta.env as Record<string, string | undefined>
export const BACKEND_URL = env.VITE_API_URL ?? 'http://localhost:8080'

/**
 * Resolve a (possibly relative) photo path returned by the backend into a full URL
 * usable in an <img src>. Absolute URLs pass through unchanged; relative paths get
 * the backend origin prepended. Returns null for missing photos so callers can
 * render a placeholder.
 */
export function resolvePhotoUrl(photoUrl: string | null | undefined): string | null {
    if (!photoUrl) return null
    if (/^https?:\/\//i.test(photoUrl)) return photoUrl
    return `${BACKEND_URL}${photoUrl.startsWith('/') ? '' : '/'}${photoUrl}`
}
