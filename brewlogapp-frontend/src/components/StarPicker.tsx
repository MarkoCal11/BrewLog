import { useState } from 'react'

interface StarDisplayProps {
    value: number
    max?: number
}

export function StarDisplay({ value, max = 5 }: StarDisplayProps) {
    return (
        <span className="star-picker" style={{ gap: 1, pointerEvents: 'none' }}>
            {Array.from({ length: max }, (_, i) => i + 1).map(n => {
                const full = value >= n
                const half = !full && value >= n - 0.5
                return (
                    <span key={n} className="star-picker-star" style={{ width: 20, height: 20 }}>
                        <span className="star-picker-glyph star-picker-empty" style={{ fontSize: 18, lineHeight: '20px' }}>☆</span>
                        {(full || half) && (
                            <span
                                className="star-picker-glyph star-picker-filled"
                                style={{ fontSize: 18, lineHeight: '20px', ...(half ? { width: '50%' } : {}) }}
                            >★</span>
                        )}
                    </span>
                )
            })}
        </span>
    )
}

interface StarPickerProps {
    value: number | null
    onChange: (value: number | null) => void
    max?: number
}

export function StarPicker({ value, onChange, max = 5 }: StarPickerProps) {
    const [hover, setHover] = useState<number | null>(null)
    const display = hover ?? value

    const pick = (v: number) => onChange(value === v ? null : v)

    return (
        <div className="star-picker" onMouseLeave={() => setHover(null)}>
            {Array.from({ length: max }, (_, i) => i + 1).map(n => {
                const halfVal = Math.max(1, n - 0.5)
                const full = display != null && display >= n
                const half = !full && display != null && display >= n - 0.5

                return (
                    <span key={n} className="star-picker-star">
                        <span className="star-picker-glyph star-picker-empty">☆</span>
                        {(full || half) && (
                            <span
                                className="star-picker-glyph star-picker-filled"
                                style={half ? { width: '50%' } : undefined}
                            >★</span>
                        )}
                        <button
                            type="button"
                            className="star-picker-zone star-picker-zone-left"
                            onMouseEnter={() => setHover(halfVal)}
                            onClick={() => pick(halfVal)}
                            aria-label={`${halfVal} stars`}
                        />
                        <button
                            type="button"
                            className="star-picker-zone star-picker-zone-right"
                            onMouseEnter={() => setHover(n)}
                            onClick={() => pick(n)}
                            aria-label={`${n} star${n === 1 ? '' : 's'}`}
                        />
                    </span>
                )
            })}
            {value != null && (
                <button type="button" className="btn btn-ghost" onClick={() => onChange(null)}>
                    Clear
                </button>
            )}
        </div>
    )
}
