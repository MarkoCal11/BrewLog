import type { ReactNode } from 'react'

type ListToolbarProps = {
    search: string
    onSearchChange: (value: string) => void
    searchPlaceholder: string
    /** Filter/sort controls — typically <label className="control">…</label> blocks. */
    children?: ReactNode
    /** Shown only when something is filtering/sorting away from the default. */
    onClear?: () => void
    showClear?: boolean
}

export function ListToolbar({
    search,
    onSearchChange,
    searchPlaceholder,
    children,
    onClear,
    showClear,
}: ListToolbarProps) {
    return (
        <div className="list-controls">
            <input
                className="input"
                type="search"
                placeholder={searchPlaceholder}
                value={search}
                onChange={e => onSearchChange(e.target.value)}
            />
            {(children || showClear) && (
                <div className="controls-row">
                    {children}
                    {showClear && onClear && (
                        <button type="button" className="btn btn-ghost btn-clear" onClick={onClear}>
                            Clear
                        </button>
                    )}
                </div>
            )}
        </div>
    )
}
