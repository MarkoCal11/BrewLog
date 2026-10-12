
export const formatPrice = (price: number | null | undefined): string | null =>
    price == null ? null : `${price.toFixed(2)}€`

// 'WET_HULLED' → 'Wet hulled'
export const formatProcessingMethod = (pm: string | null | undefined): string | null =>
    pm == null || pm === ''
        ? null
        : pm.toLowerCase().replace(/_/g, ' ').replace(/^./, c => c.toUpperCase())

// Normalize bag price by weight so beans are comparable.
export const pricePer100g = (
    price: number | null | undefined,
    weightGrams: number | null | undefined,
): number | null => {
    if (price == null || weightGrams == null || weightGrams <= 0) return null
    return (price / weightGrams) * 100
}

export const formatPricePer100g = (
    price: number | null | undefined,
    weightGrams: number | null | undefined,
): string | null => {
    const value = pricePer100g(price, weightGrams)
    return value == null ? null : `${value.toFixed(2)} €/100g`
}

// Format an already-computed €/100g figure (e.g. an average).
export const formatPer100gValue = (value: number | null | undefined): string | null =>
    value == null ? null : `${value.toFixed(2)} €/100g`

// Cost of a single brew = dose (g) × the bean's price per gram.
export const brewCost = (
    doseGrams: number | null | undefined,
    price: number | null | undefined,
    weightGrams: number | null | undefined,
): number | null => {
    const per100 = pricePer100g(price, weightGrams)
    if (per100 == null || doseGrams == null) return null
    return (doseGrams / 100) * per100
}

export const formatAltitude = (altitude: number | null | undefined): string | null =>
    altitude == null ? null : `${altitude} masl`

export const daysSinceRoast = (roastDate: string | null | undefined): number | null => {
    if (!roastDate) return null
    const roasted = new Date(roastDate)
    if (Number.isNaN(roasted.getTime())) return null
    const ms = Date.now() - roasted.getTime()
    if (ms < 0) return null
    return Math.floor(ms / 86_400_000)
}

export const formatFreshness = (roastDate: string | null | undefined): string | null => {
    const days = daysSinceRoast(roastDate)
    if (days == null) return null
    if (days === 0) return 'roasted today'
    if (days === 1) return 'roasted 1 day ago'
    return `roasted ${days} days ago`
}
