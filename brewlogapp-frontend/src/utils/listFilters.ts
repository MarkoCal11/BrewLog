import type { CoffeeBean } from '../types/CoffeeBean'
import type { BrewEntry } from '../types/BrewEntry'
import { formatMethod } from '../types/BrewEntry'
import { daysSinceRoast, pricePer100g } from './beanFormat'

// Push missing values to the end regardless of sort direction.
const nullsLast = (a: number | null, b: number | null, cmp: (x: number, y: number) => number) => {
    if (a == null && b == null) return 0
    if (a == null) return 1
    if (b == null) return -1
    return cmp(a, b)
}

export type BeanSort = 'name' | 'rating' | 'brews' | 'fresh' | 'price' | 'remaining'

export type BeanFilters = {
    search: string
    roast: string
    method: string
    note: string        // flavor note id (as string), '' = all
    fav: boolean
    owner?: string       // owner username, admin lists only; '' = all
    sort: BeanSort
    // beanId → estimated remaining grams; required for the 'remaining' sort.
    remainingByBean?: Map<number, number>
}

export function filterSortBeans(beans: CoffeeBean[], f: BeanFilters): CoffeeBean[] {
    const term = f.search.trim().toLowerCase()
    const filtered = beans.filter(bean => {
        if (f.roast && bean.roastLevel !== f.roast) return false
        if (f.method && bean.processingMethod !== f.method) return false
        if (f.fav && !bean.favorite) return false
        if (f.note && !(bean.flavorNotes ?? []).some(n => String(n.id) === f.note)) return false
        if (f.owner && bean.ownerUsername !== f.owner) return false
        if (term) {
            const haystack = [
                bean.name, bean.origin, bean.roasterName, bean.tastingNotes, bean.ownerUsername,
                ...(bean.flavorNotes ?? []).map(n => n.name),
            ].filter(Boolean).join(' ').toLowerCase()
            if (!haystack.includes(term)) return false
        }
        return true
    })
    return filtered.sort((a, b) => {
        switch (f.sort) {
            case 'rating': return b.averageRating - a.averageRating
            case 'brews': return b.brewCount - a.brewCount
            case 'fresh': return nullsLast(daysSinceRoast(a.roastDate), daysSinceRoast(b.roastDate), (x, y) => x - y)
            case 'price': return nullsLast(pricePer100g(a.price, a.weightGrams), pricePer100g(b.price, b.weightGrams), (x, y) => x - y)
            case 'remaining': return nullsLast(f.remainingByBean?.get(a.id) ?? null, f.remainingByBean?.get(b.id) ?? null, (x, y) => x - y)
            default: return a.name.localeCompare(b.name)
        }
    })
}

export type BrewSort = 'recent' | 'oldest' | 'rating'

export type BrewFilters = {
    search: string
    method: string
    bean: string         // bean id (as string), '' = all
    owner?: string        // owner username, admin lists only; '' = all
    sort: BrewSort
}

export function filterSortBrews(brews: BrewEntry[], f: BrewFilters): BrewEntry[] {
    const term = f.search.trim().toLowerCase()
    const filtered = brews.filter(entry => {
        if (f.method && entry.method !== f.method) return false
        if (f.bean && String(entry.coffeeBean?.id) !== f.bean) return false
        if (f.owner && entry.ownerUsername !== f.owner) return false
        if (term) {
            const haystack = [entry.coffeeBean?.name, entry.notes, formatMethod(entry.method), entry.ownerUsername]
                .filter(Boolean).join(' ').toLowerCase()
            if (!haystack.includes(term)) return false
        }
        return true
    })
    return filtered.sort((a, b) => {
        switch (f.sort) {
            case 'oldest': return a.brewedAt.localeCompare(b.brewedAt)
            case 'rating': return (b.rating ?? -1) - (a.rating ?? -1)
            default: return b.brewedAt.localeCompare(a.brewedAt)
        }
    })
}
