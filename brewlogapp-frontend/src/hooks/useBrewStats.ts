import { useMemo } from 'react'
import { useBrewEntries } from './useBrewEntries'
import { useCoffeeBeans } from './useCoffeeBeans'
import { useAdminBrewEntries } from './useAdminBrewEntries'
import { useAdminCoffeeBeans } from './useAdminCoffeeBeans'
import { useBeanFlavorNotes } from './useBeanFlavorNotes'
import { computeBrewStats } from '../utils/brewStats'
import type { BrewEntry } from '../types/BrewEntry'
import type { CoffeeBean } from '../types/CoffeeBean'
import type { FlavorNote } from '../types/FlavorNote'
import type { BrewStats } from '../utils/brewStats'

type UseBrewStatsResult = {
    stats: BrewStats
    beans: CoffeeBean[] | null
    brews: BrewEntry[] | null
    loading: boolean
    error: Error | null
}

/** Memoizes the pure stats computation over whatever data source feeds it. */
function useStatsFrom(
    brews: BrewEntry[] | null,
    beans: CoffeeBean[] | null,
    loading: boolean,
    error: Error | null,
    notesByBeanId: Record<number, FlavorNote[]> = {},
): UseBrewStatsResult {
    const stats = useMemo(
        () => computeBrewStats(brews ?? [], beans ?? [], notesByBeanId),
        [brews, beans, notesByBeanId],
    )
    return { stats, beans, brews, loading, error }
}

/**
 * Per-user stats. Powers the home dashboard (teaser) and the personal
 * `/analytics` page; both read this so their numbers never drift.
 */
export function useBrewStats(): UseBrewStatsResult {
    const { data: brews, loading: brewsLoading, error: brewsError } = useBrewEntries()
    const { data: beans, loading: beansLoading, error: beansError } = useCoffeeBeans()
    const notesByBeanId = useBeanFlavorNotes(beans)
    return useStatsFrom(brews, beans, brewsLoading || beansLoading, brewsError ?? beansError, notesByBeanId)
}

/**
 * Global stats across all users (admin only). Same computation, fed by the
 * admin endpoints instead of the user-scoped ones.
 */
export function useAdminBrewStats(): UseBrewStatsResult {
    const { data: brews, loading: brewsLoading, error: brewsError } = useAdminBrewEntries()
    const { data: beans, loading: beansLoading, error: beansError } = useAdminCoffeeBeans()
    return useStatsFrom(brews, beans, brewsLoading || beansLoading, brewsError ?? beansError)
}
