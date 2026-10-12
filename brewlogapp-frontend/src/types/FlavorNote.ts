export interface FlavorNote {
    id: number
    name: string
    category: string
    // true = shared system note (admin-created); false/absent = caller's personal note.
    global?: boolean
}

// 'FRUITY' → 'Fruity'. Categories are free-text but conventionally single-word enums.
export const formatCategory = (category: string): string =>
    category ? category.charAt(0).toUpperCase() + category.slice(1).toLowerCase() : category