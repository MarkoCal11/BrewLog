export interface CafeLocation {
    id: number
    name: string
    city: string | null
    address: string | null
    // Manual 1–5 star rating, or null when unrated.
    rating: number | null
    // Instagram handle or URL as entered; normalize with instagramUrl() for links.
    instagram: string | null
    // Map coordinates for the pin. Null until the user drops a pin on the map.
    latitude: number | null
    longitude: number | null
    // Only present on (future) admin responses: owner of the café.
    ownerId?: number
    ownerUsername?: string
}

/** True when the café has both coordinates and can be placed on a map. */
export function hasCoords(cafe: Pick<CafeLocation, 'latitude' | 'longitude'>): boolean {
    return cafe.latitude != null && cafe.longitude != null
}

/**
 * Normalize a stored Instagram value (handle like "kaffemik", "@kaffemik", or a
 * full URL) into a usable profile URL, or null if blank. Absolute URLs pass
 * through unchanged.
 */
export function instagramUrl(instagram: string | null | undefined): string | null {
    if (!instagram) return null
    const trimmed = instagram.trim()
    if (!trimmed) return null
    if (/^https?:\/\//i.test(trimmed)) return trimmed
    const handle = trimmed.replace(/^@/, '')
    return `https://instagram.com/${handle}`
}

/** Strip a leading @ for display of an Instagram handle (URLs returned as-is). */
export function instagramLabel(instagram: string): string {
    const trimmed = instagram.trim()
    if (/^https?:\/\//i.test(trimmed)) return trimmed
    return `@${trimmed.replace(/^@/, '')}`
}

/**
 * Reduce whatever the user typed in the Instagram field to the bare lowercase handle
 * the backend stores. Mirrors the backend's normalizeInstagram so the client never
 * sends a shape the backend's stricter input regex would reject (it only accepts a
 * bare/@ handle or an `https://` instagram.com URL with no query/fragment).
 *
 * Accepts a bare/@ handle, or an instagram.com URL with any protocol (http→https is
 * irrelevant once we extract the handle), optional `www.`, and trailing slash / query
 * / fragment. Returns `{ handle: null }` for blank input, or `{ error }` when the value
 * can't be reduced to a valid handle.
 */
export function normalizeInstagramInput(raw: string): { handle: string | null; error?: string } {
    const trimmed = raw.trim()
    if (!trimmed) return { handle: null }
    const handle = trimmed
        .replace(/^(https?:\/\/)?(www\.)?instagram\.com\//i, '') // strip any instagram.com URL prefix
        .replace(/^@/, '')                                       // drop a leading @
        .split(/[/?#]/)[0]                                       // drop trailing slash / query / fragment
        .toLowerCase()
    if (!handle) return { handle: null }
    if (!/^[a-z0-9._]{1,30}$/.test(handle)) {
        return { handle: null, error: 'Enter an Instagram handle (e.g. @cafe) or an instagram.com link.' }
    }
    return { handle }
}
