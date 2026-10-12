import { useEffect, useState } from 'react'
import { getFlavorNotesForBean } from '../api/flavorNoteApi'
import type { CoffeeBean } from '../types/CoffeeBean'
import type { FlavorNote } from '../types/FlavorNote'

/**
 * Fans out the per-bean flavor-note lookups into a beanId→notes map.
 * Shared by the home dashboard and the analytics page so their flavor
 * aggregations never drift. A failed lookup degrades to an empty list.
 */
export function useBeanFlavorNotes(beans: CoffeeBean[] | null): Record<number, FlavorNote[]> {
    const [notesByBeanId, setNotesByBeanId] = useState<Record<number, FlavorNote[]>>({})

    useEffect(() => {
        if (!beans || beans.length === 0) {
            setNotesByBeanId({})
            return
        }
        let cancelled = false
        Promise.all(
            beans.map(bean =>
                getFlavorNotesForBean(bean.id)
                    .then(notes => [bean.id, notes] as const)
                    .catch(() => [bean.id, [] as FlavorNote[]] as const)
            )
        ).then(entries => {
            if (cancelled) return
            setNotesByBeanId(Object.fromEntries(entries))
        })
        return () => { cancelled = true }
    }, [beans])

    return notesByBeanId
}
