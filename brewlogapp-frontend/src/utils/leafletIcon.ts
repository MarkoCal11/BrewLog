import L from 'leaflet'
import markerIcon2x from 'leaflet/dist/images/marker-icon-2x.png'
import markerIcon from 'leaflet/dist/images/marker-icon.png'
import markerShadow from 'leaflet/dist/images/marker-shadow.png'

// Leaflet's default marker images are resolved via CSS-relative URLs that break
// under Vite's bundler (the icons silently fail to load). Patch the default icon
// to use the bundled-and-hashed asset URLs instead. Import this module once
// wherever Leaflet markers are rendered (CafeMap, LocationPicker).
let patched = false

export function ensureLeafletIcons(): void {
    if (patched) return
    patched = true
    // delete the broken getIconUrl so mergeOptions below takes effect cleanly
    delete (L.Icon.Default.prototype as { _getIconUrl?: unknown })._getIconUrl
    L.Icon.Default.mergeOptions({
        iconRetinaUrl: markerIcon2x,
        iconUrl: markerIcon,
        shadowUrl: markerShadow,
    })
}
