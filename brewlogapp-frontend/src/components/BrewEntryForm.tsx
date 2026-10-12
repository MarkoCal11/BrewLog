import { useState, useMemo } from 'react'
import type { CoffeeBean } from '../types/CoffeeBean'
import type { Method } from '../types/BrewEntry'
import { METHODS, formatMethod } from '../types/BrewEntry'
import { StarPicker } from './StarPicker'

export interface BrewEntryFormData {
    method: Method
    grindSize: number | null
    waterTempC: number | null
    brewTimeSeconds: number | null
    doseGrams: number
    waterGrams: number | null
    yieldGrams: number | null
    rating: number | null
    notes: string | null
    brewedAt: string
    coffeeBeanId: number
}

interface BrewEntryFormProps {
    beans?: CoffeeBean[]
    initialValues?: Partial<BrewEntryFormData>
    mode: 'create' | 'edit'
    onSubmit: (data: BrewEntryFormData) => Promise<void>
    onCancel: () => void
    saving?: boolean
    // Server-side save error surfaced by the parent (e.g. backend 400 "Coffee bean not found").
    error?: string | null
    // beanId → estimated remaining grams, for the over-dose caution (create flow only).
    remainingByBean?: Map<number, number>
}

type FormState = {
    method: Method
    grindSize: string
    waterTempC: string
    brewTimeMinutes: string
    brewTimeSeconds: string
    doseGrams: string
    waterGrams: string
    yieldGrams: string
    rating: number | null
    notes: string
    brewedAt: string
    coffeeBeanId: number
}

function toFormState(initial?: Partial<BrewEntryFormData>): FormState {
    // null/undefined brew time → leave the inputs blank (not "0:0")
    const totalSeconds = initial?.brewTimeSeconds ?? null
    return {
        method: initial?.method ?? 'V60',
        grindSize: initial?.grindSize != null ? String(initial.grindSize) : '',
        waterTempC: initial?.waterTempC != null ? String(initial.waterTempC) : '',
        brewTimeMinutes: totalSeconds == null ? '' : String(Math.floor(totalSeconds / 60)),
        brewTimeSeconds: totalSeconds == null ? '' : String(totalSeconds % 60),
        doseGrams: String(initial?.doseGrams ?? 18),
        waterGrams: initial?.waterGrams != null ? String(initial.waterGrams) : '',
        yieldGrams: initial?.yieldGrams != null ? String(initial.yieldGrams) : '',
        rating: initial?.rating ?? null,
        notes: initial?.notes ?? '',
        // datetime-local needs exactly YYYY-MM-DDTHH:mm; trim any seconds/Z so the
        // existing brew date actually shows (and stays editable) in the edit form.
        brewedAt: initial?.brewedAt ? initial.brewedAt.slice(0, 16) : new Date().toISOString().slice(0, 16),
        coffeeBeanId: initial?.coffeeBeanId ?? 0,
    }
}

const isEspresso = (method: Method) => method === 'ESPRESSO'

export function BrewEntryForm({ beans, initialValues, mode, onSubmit, onCancel, saving, error, remainingByBean }: BrewEntryFormProps) {
    const [form, setForm] = useState<FormState>(() => toFormState(initialValues))
    const [timeError, setTimeError] = useState<string | null>(null)

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target
        if (name === 'method' || name === 'brewTimeMinutes' || name === 'brewTimeSeconds') {
            setTimeError(null)
        }
        setForm(prev => ({
            ...prev,
            [name]: name === 'coffeeBeanId' ? Number(value) : value,
        }))
    }

    const ratio = useMemo(() => {
        const dose = parseFloat(form.doseGrams)
        if (!dose || dose <= 0) return null
        if (isEspresso(form.method)) {
            const yld = parseFloat(form.yieldGrams)
            if (!yld || yld <= 0) return null
            return `1:${(yld / dose).toFixed(1)}`
        }
        const water = parseFloat(form.waterGrams)
        if (!water || water <= 0) return null
        return `1:${(water / dose).toFixed(1)}`
    }, [form.doseGrams, form.waterGrams, form.yieldGrams, form.method])

    // Estimate-based caution (never blocks): dose exceeds what's left of the selected bag.
    const remaining = remainingByBean?.get(form.coffeeBeanId)
    const dose = parseFloat(form.doseGrams)
    const overDose = remaining != null && dose > 0 && dose > remaining

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        // Both inputs blank → no brew time recorded (null), rather than a fake 0.
        const hasTime = form.brewTimeMinutes.trim() !== '' || form.brewTimeSeconds.trim() !== ''
        const totalSeconds = hasTime
            ? (parseInt(form.brewTimeMinutes) || 0) * 60 + (parseInt(form.brewTimeSeconds) || 0)
            : null

        // Brew time is optional for every method except espresso, where shot time matters.
        if (isEspresso(form.method) && !totalSeconds) {
            setTimeError('Shot time is required for espresso.')
            return
        }
        setTimeError(null)

        if (mode === 'edit' && !window.confirm('Save changes to this brew?')) return

        await onSubmit({
            method: form.method,
            grindSize: form.grindSize.trim() === '' ? null : Number(form.grindSize),
            waterTempC: form.waterTempC.trim() === '' ? null : Number(form.waterTempC),
            brewTimeSeconds: totalSeconds,
            doseGrams: Number(form.doseGrams),
            waterGrams: isEspresso(form.method) ? null : (parseFloat(form.waterGrams) || null),
            yieldGrams: isEspresso(form.method) ? (parseFloat(form.yieldGrams) || null) : null,
            rating: form.rating,
            notes: form.notes.trim() || null,
            brewedAt: form.brewedAt,
            coffeeBeanId: form.coffeeBeanId,
        })
    }

    return (
        <form className="form" onSubmit={handleSubmit}>
            <p className="form-legend"><span className="required-mark">*</span> Required — all other fields are optional</p>

            {mode === 'create' && beans && (
                <div className="field">
                    <label className="label">Bean <span className="required-mark">*</span></label>
                    <select className="select" name="coffeeBeanId" value={form.coffeeBeanId} onChange={handleChange} required>
                        <option value={0} disabled>Select a bean</option>
                        {[...beans]
                            .sort((a, b) => Number(!!a.finished) - Number(!!b.finished))
                            .map(bean => (
                                <option key={bean.id} value={bean.id}>
                                    {bean.name}{bean.finished ? ' (finished)' : ''}
                                </option>
                            ))}
                    </select>
                </div>
            )}

            <div className="field">
                <label className="label">Method <span className="required-mark">*</span></label>
                <select className="select" name="method" value={form.method} onChange={handleChange}>
                    {METHODS.map(m => <option key={m} value={m}>{formatMethod(m)}</option>)}
                </select>
            </div>

            <div className="field-row">
                <div className="field">
                    <label className="label">Dose (g) <span className="required-mark">*</span></label>
                    <input className="input" type="number" step="0.1" min="0" name="doseGrams" value={form.doseGrams} onChange={handleChange} required />
                </div>
                {isEspresso(form.method) ? (
                    <div className="field">
                        <label className="label">Yield (g)</label>
                        <input className="input" type="number" step="0.1" min="0" name="yieldGrams" value={form.yieldGrams} onChange={handleChange} />
                    </div>
                ) : (
                    <div className="field">
                        <label className="label">Water (g)</label>
                        <input className="input" type="number" step="0.1" min="0" name="waterGrams" value={form.waterGrams} onChange={handleChange} />
                    </div>
                )}
                {ratio && <div className="ratio-badge">{ratio}</div>}
            </div>

            {overDose && remaining != null && (
                <span className="field-warn">
                    Only ~{Math.round(remaining)}g left in this bag — logging this will use more than that.
                </span>
            )}

            <div className="field-row">
                <div className="field">
                    <label className="label">Grind size</label>
                    <input className="input" type="number" step="0.1" min="0" name="grindSize" value={form.grindSize} onChange={handleChange} />
                </div>
                <div className="field">
                    <label className="label">Water temp ({'°'}C)</label>
                    <input className="input" type="number" step="0.1" min="0" name="waterTempC" value={form.waterTempC} onChange={handleChange} />
                </div>
            </div>

            <div className="field">
                <label className="label">
                    {isEspresso(form.method) ? <>Shot time <span className="required-mark">*</span></> : 'Brew time'}
                </label>
                <div className="time-input">
                    <input className="input" type="number" step="1" min="0" name="brewTimeMinutes" value={form.brewTimeMinutes} onChange={handleChange} placeholder="min" />
                    <span className="time-separator">:</span>
                    <input className="input" type="number" step="1" min="0" max="59" name="brewTimeSeconds" value={form.brewTimeSeconds} onChange={handleChange} placeholder="sec" />
                </div>
                {timeError && <span className="error-inline">{timeError}</span>}
            </div>

            <div className="field">
                <label className="label">Rating</label>
                <StarPicker value={form.rating} onChange={r => setForm(prev => ({ ...prev, rating: r }))} />
            </div>

            <div className="field">
                <label className="label">Brewed at <span className="required-mark">*</span></label>
                <input className="input" type="datetime-local" name="brewedAt" value={form.brewedAt} onChange={handleChange} required />
            </div>

            <div className="field">
                <label className="label">Notes</label>
                <textarea className="input" name="notes" value={form.notes} onChange={handleChange} maxLength={100} rows={3} placeholder="Tasting notes, adjustments..." />
                {form.notes.length > 0 && (
                    <span className="field-hint">{form.notes.length}/500</span>
                )}
            </div>

            <div className="field-row" style={{ marginTop: 4 }}>
                <button type="button" className="btn btn-ghost btn-block" onClick={onCancel}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-block" disabled={saving || (mode === 'create' && form.coffeeBeanId === 0)}>
                    {saving ? 'Saving...' : mode === 'create' ? 'Log Brew' : 'Save Changes'}
                </button>
            </div>
            {error && <div className="error-inline">{error}</div>}
        </form>
    )
}
