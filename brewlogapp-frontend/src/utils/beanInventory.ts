import type { BrewEntry } from '../types/BrewEntry'
import type { CoffeeBean } from '../types/CoffeeBean'

export type BeanConsumption = { consumed: number; count: number }

// Sum doseGrams per bean across all brews, in a single pass.
// Brews without a dose are ignored (they can't tell us how much was used).
export function consumptionByBean(brews: BrewEntry[]): Map<number, BeanConsumption> {
    const map = new Map<number, BeanConsumption>()
    for (const brew of brews) {
        const beanId = brew.coffeeBean?.id
        if (beanId == null || brew.doseGrams == null) continue
        const cur = map.get(beanId) ?? { consumed: 0, count: 0 }
        map.set(beanId, { consumed: cur.consumed + brew.doseGrams, count: cur.count + 1 })
    }
    return map
}

export type BeanInventory = {
    bagGrams: number | null         // null when the bean has no weightGrams
    consumedGrams: number
    remainingGrams: number | null   // null when bag weight unknown; never negative
    percentRemaining: number | null
    avgDoseGrams: number | null
    brewsLeft: number | null        // estimate, from avg dose
    low: boolean                    // known remaining ≤ 15% (and not empty)
    empty: boolean                  // known remaining ≤ 0
}

const LOW_THRESHOLD = 0.15

export function computeInventory(bean: CoffeeBean, consumption?: BeanConsumption): BeanInventory {
    const consumed = consumption?.consumed ?? 0
    const count = consumption?.count ?? 0
    const bag = bean.weightGrams ?? null
    const remaining = bag != null ? Math.max(bag - consumed, 0) : null
    const percent = bag != null && bag > 0 && remaining != null ? (remaining / bag) * 100 : null
    const avgDose = count > 0 ? consumed / count : null
    const brewsLeft = remaining != null && avgDose != null && avgDose > 0 ? Math.floor(remaining / avgDose) : null
    return {
        bagGrams: bag,
        consumedGrams: consumed,
        remainingGrams: remaining,
        percentRemaining: percent,
        avgDoseGrams: avgDose,
        brewsLeft,
        low: remaining != null && bag != null && bag > 0 && remaining > 0 && remaining <= bag * LOW_THRESHOLD,
        empty: remaining != null && remaining <= 0,
    }
}

export type BeanStockStatus = 'finished' | 'empty' | 'low' | null

// Manual finished flag wins; otherwise fall back to the computed level.
export function stockStatus(bean: CoffeeBean, inv: BeanInventory): BeanStockStatus {
    if (bean.finished) return 'finished'
    if (inv.empty) return 'empty'
    if (inv.low) return 'low'
    return null
}

export const formatGrams = (g: number) => `${Math.round(g)}g`
