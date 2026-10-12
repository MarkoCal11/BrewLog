export type AuditAction = 'DEACTIVATE' | 'REACTIVATE' | 'ROLE_CHANGE'

export interface AuditLog {
    id: number
    actorUsername: string
    targetUserId: number
    // Null only if the target couldn't be resolved (LEFT JOIN); users aren't hard-deleted.
    targetUsername: string | null
    action: AuditAction
    // For ROLE_CHANGE these are role names (USER/ADMIN); for de/reactivate they're "true"/"false".
    oldValue: string | null
    newValue: string | null
    createdAt: string
}

/** The slice of Spring's Page<T> JSON the frontend consumes. */
export interface Page<T> {
    content: T[]
    totalElements: number
    totalPages: number
    number: number
    size: number
    first: boolean
    last: boolean
}
