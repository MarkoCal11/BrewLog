import type { BrewEntry, Method } from '../types/BrewEntry'
import type { CoffeeBean, ProcessingMethod } from '../types/CoffeeBean'
import { ROAST_LEVELS } from '../types/CoffeeBean'
import type { FlavorNote } from '../types/FlavorNote'
import { pricePer100g, brewCost } from './beanFormat'

const MS_PER_DAY = 86_400_000

export type MethodStat = {
    method: Method
    count: number
    avgRating: number | null
    avgGrind: number | null
    avgTempC: number | null
    avgTimeSeconds: number | null
}

export type MonthStat = {
    month: string // 'YYYY-MM'
    label: string // 'May 2026'
    count: number
    avgRating: number | null
}

export type RatioStat = {
    avgBrewRatio: number | null // water ÷ dose
    brewRatioCoverage: number // brews with both fields
    avgYieldRatio: number | null // yield ÷ dose
    yieldRatioCoverage: number
}

export type RoastLevelStat = {
    level: CoffeeBean['roastLevel']
    count: number
}

export type LabelCount = {
    label: string
    count: number
}

export type ProcessingStat = {
    method: ProcessingMethod
    count: number
    avgRating: number | null
}

export type FlavorStat = {
    id: number
    name: string
    count: number // brews tagged with this note
    avgRating: number | null
}

export type CategoryStat = {
    category: string
    count: number // brews tagged with a note in this category
    avgRating: number | null
}

export type BrewStats = {
    // brews
    totalBrews: number
    ratedCount: number
    avgRating: number | null
    byMethod: MethodStat[] // sorted desc by count
    topMethod: MethodStat | null
    byProcessingMethod: ProcessingStat[] // brews grouped by their bean's processing method, desc by count
    flavorStats: FlavorStat[] // desc by count; empty unless notesByBeanId supplied
    categoryStats: CategoryStat[] // desc by count; empty unless notesByBeanId supplied
    recentBrews: BrewEntry[] // newest first, max 3
    ratios: RatioStat
    avgCostPerBrew: number | null // dose × bean price-per-gram
    totalBrewCost: number | null
    costCoverage: number // brews whose bean had price + weight
    avgDaysOffRoast: number | null // days between bean roast date and brew time
    daysOffRoastCoverage: number // brews whose bean had a roast date
    byMonth: MonthStat[] // chronological
    // beans
    totalBeans: number
    favoriteCount: number
    untriedCount: number // brewCount === 0
    topBean: CoffeeBean | null
    roastLevels: RoastLevelStat[]
    topOrigins: LabelCount[]
    topRoasters: LabelCount[]
    avgPricePer100g: number | null
}

const mean = (nums: number[]): number | null =>
    nums.length === 0 ? null : nums.reduce((a, b) => a + b, 0) / nums.length

const ratedValues = (brews: BrewEntry[]): number[] =>
    brews.filter(b => b.rating != null).map(b => b.rating as number)

function topLabelCounts(values: string[], limit: number): LabelCount[] {
    const counts = new Map<string, number>()
    for (const v of values) counts.set(v, (counts.get(v) ?? 0) + 1)
    return [...counts.entries()]
        .map(([label, count]) => ({ label, count }))
        .sort((a, b) => b.count - a.count)
        .slice(0, limit)
}

export function computeBrewStats(
    brews: BrewEntry[],
    beans: CoffeeBean[],
    notesByBeanId: Record<number, FlavorNote[]> = {},
): BrewStats {
    // --- brews by method ---
    const methodGroups = new Map<Method, BrewEntry[]>()
    for (const brew of brews) {
        const list = methodGroups.get(brew.method) ?? []
        list.push(brew)
        methodGroups.set(brew.method, list)
    }
    const byMethod: MethodStat[] = [...methodGroups.entries()]
        .map(([method, list]) => ({
            method,
            count: list.length,
            avgRating: mean(ratedValues(list)),
            avgGrind: mean(list.filter(b => b.grindSize != null).map(b => b.grindSize as number)),
            avgTempC: mean(list.filter(b => b.waterTempC != null).map(b => b.waterTempC as number)),
            avgTimeSeconds: mean(
                list.filter(b => b.brewTimeSeconds != null).map(b => b.brewTimeSeconds as number),
            ),
        }))
        .sort((a, b) => b.count - a.count)

    // --- brews by the bean's processing method (skips beans with no method) ---
    const processingGroups = new Map<ProcessingMethod, BrewEntry[]>()
    for (const brew of brews) {
        const pm = brew.coffeeBean?.processingMethod
        if (!pm) continue
        const list = processingGroups.get(pm) ?? []
        list.push(brew)
        processingGroups.set(pm, list)
    }
    const byProcessingMethod: ProcessingStat[] = [...processingGroups.entries()]
        .map(([method, list]) => ({ method, count: list.length, avgRating: mean(ratedValues(list)) }))
        .sort((a, b) => b.count - a.count)

    // --- flavor notes & categories (only when the per-bean note map is supplied) ---
    const flavorAgg = new Map<number, { name: string; count: number; ratings: number[] }>()
    const categoryAgg = new Map<string, { count: number; ratings: number[] }>()
    for (const brew of brews) {
        const beanId = brew.coffeeBean?.id
        if (beanId == null) continue
        for (const note of notesByBeanId[beanId] ?? []) {
            const f = flavorAgg.get(note.id) ?? { name: note.name, count: 0, ratings: [] }
            f.count += 1
            if (brew.rating != null) f.ratings.push(brew.rating)
            flavorAgg.set(note.id, f)

            const c = categoryAgg.get(note.category) ?? { count: 0, ratings: [] }
            c.count += 1
            if (brew.rating != null) c.ratings.push(brew.rating)
            categoryAgg.set(note.category, c)
        }
    }
    const flavorStats: FlavorStat[] = [...flavorAgg.entries()]
        .map(([id, v]) => ({ id, name: v.name, count: v.count, avgRating: mean(v.ratings) }))
        .sort((a, b) => b.count - a.count || (b.avgRating ?? 0) - (a.avgRating ?? 0))
    const categoryStats: CategoryStat[] = [...categoryAgg.entries()]
        .map(([category, v]) => ({ category, count: v.count, avgRating: mean(v.ratings) }))
        .sort((a, b) => b.count - a.count || (b.avgRating ?? 0) - (a.avgRating ?? 0))

    // --- ratios (only over brews where the inputs exist) ---
    const brewRatios = brews
        .filter(b => b.waterGrams != null && b.doseGrams != null && b.doseGrams > 0)
        .map(b => (b.waterGrams as number) / (b.doseGrams as number))
    const yieldRatios = brews
        .filter(b => b.yieldGrams != null && b.doseGrams != null && b.doseGrams > 0)
        .map(b => (b.yieldGrams as number) / (b.doseGrams as number))

    // --- per-brew cost: dose (g) × bean's price per gram ---
    const brewCosts = brews
        .map(b => brewCost(b.doseGrams, b.coffeeBean?.price, b.coffeeBean?.weightGrams))
        .filter((v): v is number => v != null)

    // --- freshness at brew: days between the bean's roast date and the brew ---
    const daysOffRoast = brews
        .map(b => {
            const roastDate = b.coffeeBean?.roastDate
            if (!roastDate) return null
            const ms = new Date(b.brewedAt).getTime() - new Date(roastDate).getTime()
            return Number.isNaN(ms) || ms < 0 ? null : Math.floor(ms / MS_PER_DAY)
        })
        .filter((v): v is number => v != null)

    // --- brews by month ---
    const monthGroups = new Map<string, BrewEntry[]>()
    for (const brew of brews) {
        const month = brew.brewedAt.slice(0, 7) // 'YYYY-MM'
        const list = monthGroups.get(month) ?? []
        list.push(brew)
        monthGroups.set(month, list)
    }
    const byMonth: MonthStat[] = [...monthGroups.entries()]
        .sort((a, b) => a[0].localeCompare(b[0]))
        .map(([month, list]) => ({
            month,
            label: new Date(`${month}-01T00:00:00`).toLocaleDateString(undefined, {
                month: 'short',
                year: 'numeric',
            }),
            count: list.length,
            avgRating: mean(ratedValues(list)),
        }))

    // --- beans ---
    const triedBeans = beans.filter(b => b.brewCount > 0)
    const topBean =
        triedBeans.slice().sort((a, b) => b.averageRating - a.averageRating)[0] ?? null
    const roastLevels: RoastLevelStat[] = ROAST_LEVELS.map(level => ({
        level,
        count: beans.filter(b => b.roastLevel === level).length,
    }))

    return {
        totalBrews: brews.length,
        ratedCount: ratedValues(brews).length,
        avgRating: mean(ratedValues(brews)),
        byMethod,
        topMethod: byMethod[0] ?? null,
        byProcessingMethod,
        flavorStats,
        categoryStats,
        recentBrews: brews
            .slice()
            .sort((a, b) => b.brewedAt.localeCompare(a.brewedAt))
            .slice(0, 3),
        ratios: {
            avgBrewRatio: mean(brewRatios),
            brewRatioCoverage: brewRatios.length,
            avgYieldRatio: mean(yieldRatios),
            yieldRatioCoverage: yieldRatios.length,
        },
        avgCostPerBrew: mean(brewCosts),
        totalBrewCost: brewCosts.length ? brewCosts.reduce((a, b) => a + b, 0) : null,
        costCoverage: brewCosts.length,
        avgDaysOffRoast: mean(daysOffRoast),
        daysOffRoastCoverage: daysOffRoast.length,
        byMonth,
        totalBeans: beans.length,
        favoriteCount: beans.filter(b => b.favorite).length,
        untriedCount: beans.filter(b => b.brewCount === 0).length,
        topBean,
        roastLevels,
        topOrigins: topLabelCounts(beans.map(b => b.origin), 5),
        topRoasters: topLabelCounts(beans.map(b => b.roasterName), 5),
        avgPricePer100g: mean(
            beans.map(b => pricePer100g(b.price, b.weightGrams)).filter((v): v is number => v != null),
        ),
    }
}
