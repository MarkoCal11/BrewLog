import { useMemo } from 'react'
import { MapContainer, TileLayer, Marker, useMapEvents } from 'react-leaflet'
import type { LeafletMouseEvent, Marker as LeafletMarker } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { ensureLeafletIcons } from '../utils/leafletIcon'

ensureLeafletIcons()

// Fallback view when no pin is set yet (Zagreb). The user pans/zooms to
// their city, then clicks to drop the pin.
const DEFAULT_CENTER: [number, number] = [45.8131, 15.9775]
const DEFAULT_ZOOM = 12

type LocationPickerProps = {
    lat: number | null
    lng: number | null
    onChange: (lat: number, lng: number) => void
}

// Click anywhere on the map to (re)place the pin.
function ClickToPlace({ onChange }: { onChange: (lat: number, lng: number) => void }) {
    useMapEvents({
        click(e: LeafletMouseEvent) {
            onChange(e.latlng.lat, e.latlng.lng)
        },
    })
    return null
}

export function LocationPicker({ lat, lng, onChange }: LocationPickerProps) {
    const hasPin = lat != null && lng != null
    const center = useMemo<[number, number]>(
        () => (hasPin ? [lat as number, lng as number] : DEFAULT_CENTER),
        // Only set the initial center; don't re-center as the user drags the pin.
        // eslint-disable-next-line react-hooks/exhaustive-deps
        [],
    )

    return (
        <MapContainer center={center} zoom={hasPin ? 15 : DEFAULT_ZOOM} className="cafe-map-pick">
            <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <ClickToPlace onChange={onChange} />
            {hasPin && (
                <Marker
                    position={[lat as number, lng as number]}
                    draggable
                    eventHandlers={{
                        dragend(e) {
                            const marker = e.target as LeafletMarker
                            const pos = marker.getLatLng()
                            onChange(pos.lat, pos.lng)
                        },
                    }}
                />
            )}
        </MapContainer>
    )
}
