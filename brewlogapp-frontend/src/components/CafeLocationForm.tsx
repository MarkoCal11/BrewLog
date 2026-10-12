import { useState } from 'react'
import type { CafeLocation } from '../types/CafeLocation'
import { normalizeInstagramInput } from '../types/CafeLocation'
import type { CafeLocationCreateRequest } from '../api/cafeLocationApi'
import type { GeocodeResult } from '../api/geocode'
import { LocationPicker } from './LocationPicker'
import { AddressAutocomplete } from './AddressAutocomplete'
import { StarPicker } from './StarPicker'

type FormState = {
    name: string
    city: string
    address: string
    rating: number | null
    instagram: string
    latitude: number | null
    longitude: number | null
}

const emptyState = (): FormState => ({
    name: '',
    city: '',
    address: '',
    rating: null,
    instagram: '',
    latitude: null,
    longitude: null,
})

function toFormState(cafe: CafeLocation): FormState {
    return {
        name: cafe.name,
        city: cafe.city ?? '',
        address: cafe.address ?? '',
        rating: cafe.rating,
        instagram: cafe.instagram ?? '',
        latitude: cafe.latitude,
        longitude: cafe.longitude,
    }
}

interface CafeLocationFormProps {
    mode: 'create' | 'edit'
    cafe?: CafeLocation
    onSubmit: (data: CafeLocationCreateRequest) => Promise<void>
    onCancel: () => void
    saving?: boolean
    error?: string | null
}

export function CafeLocationForm({ mode, cafe, onSubmit, onCancel, saving, error }: CafeLocationFormProps) {
    const [form, setForm] = useState<FormState>(() => (cafe ? toFormState(cafe) : emptyState()))
    const [instagramError, setInstagramError] = useState<string | null>(null)

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target
        if (name === 'instagram') setInstagramError(null)
        setForm(prev => ({ ...prev, [name]: value }))
    }

    const setPin = (lat: number, lng: number) => {
        setForm(prev => ({ ...prev, latitude: lat, longitude: lng }))
    }

    // Picking an address suggestion fills the street + city and drops the map pin in one
    // go. City only overwrites when the result actually has one (keep a manual entry).
    const handleAddressSelect = (result: GeocodeResult) => {
        setForm(prev => ({
            ...prev,
            address: result.address || prev.address,
            city: result.city ?? prev.city,
            latitude: result.lat,
            longitude: result.lng,
        }))
    }

    const clearPin = () => {
        setForm(prev => ({ ...prev, latitude: null, longitude: null }))
    }

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        // Reduce the Instagram field to the bare handle the backend stores; reject input
        // (e.g. a non-instagram URL) before the round-trip with a clear message.
        const instagram = normalizeInstagramInput(form.instagram)
        if (instagram.error) {
            setInstagramError(instagram.error)
            return
        }
        if (mode === 'edit' && !window.confirm('Save changes to this café?')) return
        await onSubmit({
            name: form.name,
            city: form.city.trim() || null,
            address: form.address.trim() || null,
            rating: form.rating,
            instagram: instagram.handle,
            latitude: form.latitude,
            longitude: form.longitude,
        })
    }

    const hasPin = form.latitude != null && form.longitude != null

    return (
        <form className="form" onSubmit={handleSubmit}>
            <p className="form-legend"><span className="required-mark">*</span> Required — all other fields are optional</p>
            <div className="field">
                <label className="label">Name <span className="required-mark">*</span></label>
                <input className="input" name="name" placeholder="e.g. Quahwa" value={form.name} onChange={handleChange} required />
            </div>
            <div className="field">
                <label className="label">Instagram</label>
                <input className="input" name="instagram" placeholder="e.g. @quahwaroastery" value={form.instagram} onChange={handleChange} />
                {instagramError && <div className="error-inline">{instagramError}</div>}
            </div>
            <div className="field">
                <label className="label">City</label>
                <input className="input" name="city" placeholder="e.g. Zagreb" value={form.city} onChange={handleChange} />
            </div>
            <div className="field">
                <label className="label">Rating</label>
                <StarPicker value={form.rating} onChange={r => setForm(prev => ({ ...prev, rating: r }))} />
            </div>
            <div className="field field-full">
                <label className="label">Address</label>
                <AddressAutocomplete
                    name="address"
                    placeholder="e.g. ul. Nikole Tesle 9"
                    value={form.address}
                    onChange={value => setForm(prev => ({ ...prev, address: value }))}
                    onSelect={handleAddressSelect}
                />
                <p className="field-hint" style={{ textAlign: 'left' }}>Start typing to search — pick a result to fill the city and drop the pin.</p>
            </div>
            <div className="field field-full cafe-map-field">
                <label className="label">Location</label>
                <p className="field-hint" style={{ textAlign: 'left' }}>Click the map to drop a pin, or drag it to fine-tune.</p>
                <LocationPicker lat={form.latitude} lng={form.longitude} onChange={setPin} />
                <div className="row-between" style={{ marginTop: 6 }}>
                    <span className="cafe-coords">
                        {hasPin ? `${(form.latitude as number).toFixed(5)}, ${(form.longitude as number).toFixed(5)}` : 'No pin set'}
                    </span>
                    {hasPin && (
                        <button type="button" className="btn btn-ghost" onClick={clearPin}>Clear pin</button>
                    )}
                </div>
            </div>
            <div className="field-row" style={{ marginTop: 4 }}>
                <button type="button" className="btn btn-ghost btn-block" onClick={onCancel}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                    {saving ? 'Saving...' : mode === 'create' ? 'Add Café' : 'Save Changes'}
                </button>
            </div>
            {error && <div className="error-inline">{error}</div>}
        </form>
    )
}
