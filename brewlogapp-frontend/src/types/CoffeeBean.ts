import type { FlavorNote } from './FlavorNote'

export type RoastLevel =
    | 'LIGHT'
    | 'MEDIUM'
    | 'MEDIUM_DARK'
    | 'DARK'

export const ROAST_LEVELS: RoastLevel[] = ['LIGHT', 'MEDIUM', 'MEDIUM_DARK', 'DARK']

/** 'MEDIUM_DARK' → 'Medium-Dark', 'LIGHT' → 'Light'. */
export const formatRoastLevel = (level: RoastLevel | string): string =>
    level
        .split('_')
        .map(w => w.charAt(0) + w.slice(1).toLowerCase())
        .join('-')

export type ProcessingMethod =
    | 'WASHED'
    | 'NATURAL'
    | 'HONEY'
    | 'ANAEROBIC'
    | 'WET_HULLED'
    | 'PULPED_NATURAL'
    | 'CARBONIC_MACERATION'
    | 'OTHER'

export const PROCESSING_METHODS: ProcessingMethod[] = [
    'WASHED', 'NATURAL', 'HONEY', 'ANAEROBIC',
    'WET_HULLED', 'PULPED_NATURAL', 'CARBONIC_MACERATION', 'OTHER',
]

export interface CoffeeBean {
    id: number
    name: string
    origin: string
    roastLevel: RoastLevel
    processingMethod: ProcessingMethod | null
    roasterName: string
    averageRating: number
    brewCount: number
    roastDate: string | null
    price: number | null
    weightGrams: number | null
    altitude: number | null
    tastingNotes: string | null
    favorite: boolean
    // Manual "bag finished" flag (backend boolean, like favorite). Optional until the backend column ships.
    finished?: boolean
    // Relative path to the bean's photo (e.g. "/uploads/beans/12.jpg"), or null/absent if none.
    // Resolve with resolvePhotoUrl() before using in <img src>. Optional until the backend ships it.
    photoUrl?: string | null
    // Linked flavor notes (m:n). Returned on the list/detail + admin responses.
    flavorNotes?: FlavorNote[]
    // Only present on admin responses (AdminCoffeeBeanResponse): owner of the bean.
    ownerId?: number
    ownerUsername?: string
    }
