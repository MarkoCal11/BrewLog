import { useEffect, useRef, useState } from 'react'
import { searchPlaces, GEOCODE_MIN_CHARS, type GeocodeResult } from '../api/geocode'

interface AddressAutocompleteProps {
    // Controlled address text.
    value: string
    name?: string
    placeholder?: string
    // Free typing (no suggestion chosen yet).
    onChange: (value: string) => void
    // A suggestion was picked — caller fills address + city + map pin from it.
    onSelect: (result: GeocodeResult) => void
}

const DEBOUNCE_MS = 350

export function AddressAutocomplete({ value, name, placeholder, onChange, onSelect }: AddressAutocompleteProps) {
    const [results, setResults] = useState<GeocodeResult[]>([])
    const [open, setOpen] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState(false)
    const [activeIndex, setActiveIndex] = useState(-1)

    // Skip the debounced search on the value change caused by choosing a suggestion,
    // so picking a result doesn't immediately reopen the dropdown.
    const skipNextSearch = useRef(false)

    useEffect(() => {
        if (skipNextSearch.current) {
            skipNextSearch.current = false
            return
        }
        const q = value.trim()
        if (q.length < GEOCODE_MIN_CHARS) {
            setResults([])
            setOpen(false)
            setLoading(false)
            return
        }

        const controller = new AbortController()
        setLoading(true)
        setError(false)
        const timer = setTimeout(() => {
            searchPlaces(q, controller.signal)
                .then(found => {
                    setResults(found)
                    setActiveIndex(-1)
                    setOpen(true)
                    setLoading(false)
                })
                .catch(err => {
                    if (err?.name === 'AbortError') return // superseded by a newer keystroke
                    setError(true)
                    setResults([])
                    setLoading(false)
                })
        }, DEBOUNCE_MS)

        return () => {
            clearTimeout(timer)
            controller.abort()
        }
    }, [value])

    const choose = (result: GeocodeResult) => {
        skipNextSearch.current = true
        setOpen(false)
        setResults([])
        setActiveIndex(-1)
        onSelect(result)
    }

    const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
        if (!open || results.length === 0) return
        if (e.key === 'ArrowDown') {
            e.preventDefault()
            setActiveIndex(i => (i + 1) % results.length)
        } else if (e.key === 'ArrowUp') {
            e.preventDefault()
            setActiveIndex(i => (i <= 0 ? results.length - 1 : i - 1))
        } else if (e.key === 'Enter') {
            if (activeIndex >= 0) {
                e.preventDefault()
                choose(results[activeIndex])
            }
        } else if (e.key === 'Escape') {
            setOpen(false)
        }
    }

    const showMenu = open && (loading || error || results.length > 0)

    return (
        <div className="autocomplete">
            <input
                className="input"
                name={name}
                placeholder={placeholder}
                value={value}
                onChange={e => onChange(e.target.value)}
                onKeyDown={handleKeyDown}
                onFocus={() => { if (results.length > 0) setOpen(true) }}
                // Delay so a click on a suggestion (which blurs the input) still registers.
                onBlur={() => setTimeout(() => setOpen(false), 150)}
                autoComplete="off"
                role="combobox"
                aria-expanded={showMenu}
                aria-autocomplete="list"
            />
            {showMenu && (
                <ul className="autocomplete-menu" role="listbox">
                    {loading && <li className="autocomplete-status">Searching…</li>}
                    {error && !loading && <li className="autocomplete-status">Couldn’t reach the address search.</li>}
                    {!loading && !error && results.map((r, i) => (
                        <li
                            key={r.id}
                            role="option"
                            aria-selected={i === activeIndex}
                            className={i === activeIndex ? 'autocomplete-option autocomplete-option-active' : 'autocomplete-option'}
                            // mousedown fires before the input's blur, so the pick isn't lost.
                            onMouseDown={e => { e.preventDefault(); choose(r) }}
                            onMouseEnter={() => setActiveIndex(i)}
                        >
                            {r.label}
                        </li>
                    ))}
                </ul>
            )}
        </div>
    )
}
