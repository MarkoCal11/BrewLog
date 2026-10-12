import type { CoffeeBean } from './CoffeeBean'

export type Method =
    | 'V60'
    | 'AEROPRESS'
    | 'ESPRESSO'
    | 'FRENCH_PRESS'
    | 'CHEMEX'
    | 'MOKA_POT'
    | 'COLD_BREW'
    | 'POUR_OVER'
    | 'DRIP'
    | 'SYPHON'
    | 'TURKISH'
    | 'OTHER'

export const METHODS: Method[] = [
    'V60', 'AEROPRESS', 'ESPRESSO', 'FRENCH_PRESS', 'CHEMEX',
    'MOKA_POT', 'COLD_BREW', 'POUR_OVER', 'DRIP', 'SYPHON', 'TURKISH', 'OTHER',
]

export const formatMethod = (method: Method): string => method.replace(/_/g, ' ')

export interface BrewEntry {
    id: number
    method: Method
    grindSize: number | null
    waterTempC: number | null
    rating: number | null
    brewedAt: string
    coffeeBean: CoffeeBean
    brewTimeSeconds: number | null
    doseGrams: number | null
    waterGrams: number | null
    yieldGrams: number | null
    notes: string | null
    // Only present on admin responses (AdminBrewEntryResponse): owner of the brew.
    ownerId?: number
    ownerUsername?: string
}
