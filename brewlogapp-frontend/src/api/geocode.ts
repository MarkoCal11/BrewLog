// Address autocomplete via Photon (Komoot's OSM-based geocoder, https://photon.komoot.io).
// Deliberately uses plain fetch, NOT the app's axios instance: axios has
// `withCredentials = true` plus a global Authorization bearer, and we must never send
// our session/cookies to a third-party host. Photon is keyless and free; we keep usage
// polite with debouncing + a minimum query length on the caller side.

const PHOTON_URL = 'https://photon.komoot.io/api/'

/** Minimum characters before we bother hitting the geocoder. */
export const GEOCODE_MIN_CHARS = 3

/** A normalized place suggestion ready to fill the café form (address + city + pin). */
export interface GeocodeResult {
    // Stable-ish key for React lists.
    id: string
    // Full human-readable line shown in the dropdown.
    label: string
    // Street line for the address field (street + house number, or the POI name).
    address: string
    // Best city-like name, or null if the result has none.
    city: string | null
    lat: number
    lng: number
}

// Subset of Photon's GeoJSON feature properties we read.
interface PhotonProps {
    osm_id?: number
    osm_type?: string
    name?: string
    housenumber?: string
    street?: string
    city?: string
    town?: string
    village?: string
    locality?: string
    district?: string
    county?: string
    state?: string
    country?: string
}

interface PhotonFeature {
    geometry?: { coordinates?: [number, number] } // [lng, lat]
    properties?: PhotonProps
}

function cityOf(p: PhotonProps): string | null {
    return p.city || p.town || p.village || p.locality || p.district || p.county || null
}

function toResult(feature: PhotonFeature, index: number): GeocodeResult | null {
    const coords = feature.geometry?.coordinates
    const p = feature.properties
    if (!coords || coords.length !== 2 || !p) return null
    const [lng, lat] = coords

    const street = [p.street, p.housenumber].filter(Boolean).join(' ')
    const address = street || p.name || ''
    const city = cityOf(p)

    // Build the dropdown label from distinct, present parts (POI name, street, city,
    // state, country) so we never repeat e.g. a name that's identical to the street.
    const parts: string[] = []
    if (p.name) parts.push(p.name)
    if (street && street !== p.name) parts.push(street)
    if (city && city !== p.name) parts.push(city)
    if (p.state && p.state !== city) parts.push(p.state)
    if (p.country) parts.push(p.country)
    const label = parts.join(', ') || address || `${lat.toFixed(4)}, ${lng.toFixed(4)}`

    return {
        id: `${p.osm_type ?? '?'}${p.osm_id ?? index}-${index}`,
        label,
        address,
        city,
        lat,
        lng,
    }
}

/**
 * Search Photon for `query`. Pass an AbortSignal so the caller can cancel a stale
 * in-flight request when the user keeps typing. Returns [] for blank/too-short queries.
 */
export async function searchPlaces(query: string, signal?: AbortSignal): Promise<GeocodeResult[]> {
    const q = query.trim()
    if (q.length < GEOCODE_MIN_CHARS) return []

    const url = `${PHOTON_URL}?q=${encodeURIComponent(q)}&limit=6`
    const res = await fetch(url, { signal })
    if (!res.ok) throw new Error(`Geocoding failed (${res.status})`)

    const body = (await res.json()) as { features?: PhotonFeature[] }
    return (body.features ?? [])
        .map(toResult)
        .filter((r): r is GeocodeResult => r != null)
}
