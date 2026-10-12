import { useEffect, useMemo } from 'react'
import { Link } from 'react-router'
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { ensureLeafletIcons } from '../utils/leafletIcon'
import type { CafeLocation } from '../types/CafeLocation'
import { hasCoords } from '../types/CafeLocation'

ensureLeafletIcons()

// Default view (Zagreb) when there are no located cafés yet.
const DEFAULT_CENTER: [number, number] = [45.8131, 15.9775]
const DEFAULT_ZOOM = 12

// Pan/zoom the map to enclose all pins whenever the set of points changes.
function FitToMarkers({ points }: { points: [number, number][] }) {
    const map = useMap()
    useEffect(() => {
        if (points.length === 0) return
        if (points.length === 1) {
            map.setView(points[0], 15)
            return
        }
        map.fitBounds(L.latLngBounds(points), { padding: [40, 40] })
    }, [map, points])
    return null
}

type CafeMapProps = {
    cafes: CafeLocation[]
    /** When false, popups omit the detail link (e.g. already on the detail page). */
    linkToDetail?: boolean
    /** Base path for the popup detail link, e.g. "/cafes" or "/admin/cafes". */
    detailBase?: string
}

export function CafeMap({ cafes, linkToDetail = true, detailBase = '/cafes' }: CafeMapProps) {
    const located = useMemo(() => cafes.filter(hasCoords), [cafes])
    const points = useMemo<[number, number][]>(
        () => located.map(c => [c.latitude as number, c.longitude as number]),
        [located],
    )

    const center = points[0] ?? DEFAULT_CENTER

    return (
        <MapContainer center={center} zoom={DEFAULT_ZOOM} className="cafe-map">
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <FitToMarkers points={points} />
            {located.map(cafe => (
                <Marker key={cafe.id} position={[cafe.latitude as number, cafe.longitude as number]}>
                    <Popup>
                        <strong>{cafe.name}</strong>
                        {cafe.city && <div>{cafe.city}</div>}
                        {linkToDetail && (
                            <Link to={`${detailBase}/${cafe.id}`} className="cafe-popup-link">View details</Link>
                        )}
                    </Popup>
                </Marker>
            ))}
        </MapContainer>
    )
}
