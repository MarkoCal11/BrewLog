import { Capacitor } from '@capacitor/core'

/**
 * Capture abstraction: prompt the user for an image and resolve to its bytes (a Blob),
 * or null if they cancel.
 *
 * This is the ONE platform-specific seam in the photo feature. On the web it uses a
 * transient file input; on a Capacitor native platform (iOS) it uses the device camera /
 * photo library via @capacitor/camera. Everything downstream (bean form, detail, upload,
 * <img> display) is identical on both platforms.
 */
export async function pickImage(): Promise<Blob | null> {
    const picked = await (Capacitor.isNativePlatform() ? pickImageNative() : pickImageWeb())
    if (!picked) return null
    return downscaleImage(picked)
}

// Longest-edge cap + JPEG quality. Matches the backend's own re-encode (1600px / 0.85),
// so downscaling here is lossless relative to what gets stored — but it shrinks a multi-MB
// camera photo to a few hundred KB BEFORE upload. That matters beyond bandwidth: the browser
// (and the iOS WebView) RESETs the connection if the server responds before a large request
// body finishes uploading — e.g. an expired-token 401 or a >15MB 413 arriving mid-upload —
// which surfaces as an opaque "Network Error" and a server-side broken pipe. A small body
// finishes before any such early response, so the client always sees the real status code.
const MAX_EDGE = 1600
const JPEG_QUALITY = 0.85

async function downscaleImage(blob: Blob): Promise<Blob> {
    try {
        // `from-image` bakes EXIF orientation into the pixels, so the canvas output is
        // upright and metadata-free; the backend then leaves orientation alone.
        const bitmap = await createImageBitmap(blob, { imageOrientation: 'from-image' })
        const scale = Math.min(1, MAX_EDGE / Math.max(bitmap.width, bitmap.height))
        const w = Math.round(bitmap.width * scale)
        const h = Math.round(bitmap.height * scale)

        const canvas = document.createElement('canvas')
        canvas.width = w
        canvas.height = h
        const ctx = canvas.getContext('2d')
        if (!ctx) return blob
        ctx.drawImage(bitmap, 0, 0, w, h)
        bitmap.close?.()

        const out = await new Promise<Blob | null>(resolve =>
            canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY),
        )
        // Keep the re-encoded version only if it actually decoded/encoded; otherwise fall
        // back to the original bytes so a quirky source can't break the upload entirely.
        return out ?? blob
    } catch {
        return blob
    }
}

// Web: a transient <input type="file"> that resolves to the chosen File (a Blob).
function pickImageWeb(): Promise<Blob | null> {
    return new Promise(resolve => {
        const input = document.createElement('input')
        input.type = 'file'
        input.accept = 'image/*'
        // Note: a cancelled file dialog fires no event in most browsers, so the promise
        // simply never resolves for that input — acceptable since a fresh input is made
        // per call and nothing downstream waits on cancellation.
        input.onchange = () => resolve(input.files?.[0] ?? null)
        input.click()
    })
}

// Native (iOS): Camera.getPhoto lets the user shoot a photo or pick from the library
// (CameraSource.Prompt). It returns a webPath we fetch into a Blob to match the web shape.
// Cancellation throws, which we map to null so callers see the same contract as the web path.
async function pickImageNative(): Promise<Blob | null> {
    try {
        const { Camera, CameraResultType, CameraSource } = await import('@capacitor/camera')
        const photo = await Camera.getPhoto({
            resultType: CameraResultType.Uri,
            source: CameraSource.Prompt,
            quality: 90,
            // Capacitor can't read EXIF off every source; the backend already fixes
            // orientation on upload, so we leave the original bytes intact here.
            correctOrientation: false,
        })
        if (!photo.webPath) return null
        const res = await fetch(photo.webPath)
        return await res.blob()
    } catch {
        // User cancelled the camera/library sheet, or denied permission.
        return null
    }
}
