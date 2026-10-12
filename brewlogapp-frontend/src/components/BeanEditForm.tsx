import { useState } from 'react'
import type { CoffeeBean, ProcessingMethod } from '../types/CoffeeBean'
import { PROCESSING_METHODS, ROAST_LEVELS, formatRoastLevel } from '../types/CoffeeBean'
import type { CoffeeBeanUpdateRequest } from '../api/coffeeBeanApi'
import { formatProcessingMethod } from '../utils/beanFormat'

type FormState = {
    name: string
    origin: string
    roastLevel: CoffeeBean['roastLevel']
    processingMethod: string
    roasterName: string
    roastDate: string
    price: string
    weightGrams: string
    altitude: string
    tastingNotes: string
}

function toFormState(bean: CoffeeBean): FormState {
    return {
        name: bean.name,
        origin: bean.origin,
        roastLevel: bean.roastLevel,
        processingMethod: bean.processingMethod ?? '',
        roasterName: bean.roasterName,
        roastDate: bean.roastDate ?? '',
        price: bean.price != null ? String(bean.price) : '',
        weightGrams: bean.weightGrams != null ? String(bean.weightGrams) : '',
        altitude: bean.altitude != null ? String(bean.altitude) : '',
        tastingNotes: bean.tastingNotes ?? '',
    }
}

interface BeanEditFormProps {
    bean: CoffeeBean
    onSubmit: (data: CoffeeBeanUpdateRequest) => Promise<void>
    onCancel: () => void
    saving?: boolean
    error?: string | null
}

export function BeanEditForm({ bean, onSubmit, onCancel, saving, error }: BeanEditFormProps) {
    const [form, setForm] = useState<FormState>(() => toFormState(bean))

    const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target
        setForm(prev => ({ ...prev, [name]: value }))
    }

    const handleSubmit = async (e: React.SyntheticEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!window.confirm('Save changes to this bean?')) return
        await onSubmit({
            name: form.name,
            origin: form.origin,
            roastLevel: form.roastLevel,
            processingMethod: (form.processingMethod as ProcessingMethod) || null,
            roasterName: form.roasterName,
            roastDate: form.roastDate || null,
            price: form.price.trim() ? Number(form.price) : null,
            weightGrams: form.weightGrams.trim() ? Number(form.weightGrams) : null,
            altitude: form.altitude.trim() ? Number(form.altitude) : null,
            tastingNotes: form.tastingNotes.trim() || null,
            // favorite/finished are intentionally NOT sent: they're managed only by the
            // dedicated PATCH toggles (star + Finished button), and the PUT no longer touches them.
        })
    }

    return (
        <form className="form" onSubmit={handleSubmit}>
            <p className="form-legend"><span className="required-mark">*</span> Required — all other fields are optional</p>
            <div className="field">
                <label className="label">Name <span className="required-mark">*</span></label>
                <input className="input" name="name" value={form.name} onChange={handleChange} required />
            </div>
            <div className="field">
                <label className="label">Roaster <span className="required-mark">*</span></label>
                <input className="input" name="roasterName" value={form.roasterName} onChange={handleChange} required />
            </div>
            <div className="field">
                <label className="label">Origin <span className="required-mark">*</span></label>
                <input className="input" name="origin" value={form.origin} onChange={handleChange} required />
            </div>
            <div className="field">
                <label className="label">Roast level <span className="required-mark">*</span></label>
                <select className="select" name="roastLevel" value={form.roastLevel} onChange={handleChange}>
                    {ROAST_LEVELS.map(r => (
                        <option key={r} value={r}>{formatRoastLevel(r)}</option>
                    ))}
                </select>
            </div>
            <div className="field">
                <label className="label">Processing method</label>
                <select className="select" name="processingMethod" value={form.processingMethod} onChange={handleChange}>
                    <option value="">—</option>
                    {PROCESSING_METHODS.map(pm => (
                        <option key={pm} value={pm}>{formatProcessingMethod(pm)}</option>
                    ))}
                </select>
            </div>
            <div className="field-row">
                <div className="field">
                    <label className="label">Roast date</label>
                    <input className="input" type="date" name="roastDate" value={form.roastDate} onChange={handleChange} />
                </div>
                <div className="field">
                    <label className="label">Altitude (masl)</label>
                    <input className="input" type="number" step="1" min="0" name="altitude" value={form.altitude} onChange={handleChange} />
                </div>
            </div>
            <div className="field-row">
                <div className="field">
                    <label className="label">Price (bag total)</label>
                    <input className="input" type="number" step="0.01" min="0" name="price" value={form.price} onChange={handleChange} />
                </div>
                <div className="field">
                    <label className="label">Bag weight (g)</label>
                    <input className="input" type="number" step="1" min="0" name="weightGrams" value={form.weightGrams} onChange={handleChange} />
                </div>
            </div>
            <div className="field">
                <label className="label">Tasting notes</label>
                <textarea className="input" name="tastingNotes" value={form.tastingNotes} onChange={handleChange} maxLength={255} rows={3} />
                {form.tastingNotes.length > 0 && (
                    <span className="field-hint">{form.tastingNotes.length}/255</span>
                )}
            </div>
            <div className="field-row" style={{ marginTop: 4 }}>
                <button type="button" className="btn btn-ghost btn-block" onClick={onCancel}>Cancel</button>
                <button type="submit" className="btn btn-primary btn-block" disabled={saving}>
                    {saving ? 'Saving...' : 'Save Changes'}
                </button>
            </div>
            {error && <div className="error-inline">{error}</div>}
        </form>
    )
}
