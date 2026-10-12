import axios from 'axios'

/**
 * Pull a human-readable message out of an unknown error.
 * Prefers a message sent by the backend (string body, or `{ message }` JSON),
 * then an optional domain-specific `fallback`, then the raw error message.
 *
 * Note: Spring Boot omits exception messages from error bodies by default
 * (server.error.include-message=never), so the `fallback` is what users will
 * usually see unless the backend explicitly returns a message.
 */
export const extractErrorMessage = (err: unknown, fallback?: string): string => {
    if (axios.isAxiosError(err)) {
        const data = err.response?.data
        if (typeof data === 'string' && data.length > 0) return data
        if (data && typeof data === 'object') {
            const message = (data as { message?: unknown }).message
            if (typeof message === 'string' && message.length > 0) return message
        }
        // Only assume the domain fallback when the server actually responded with an
        // error (a deliberate rejection). For network/timeout errors (no response),
        // surface the real error instead of guessing at a cause.
        if (fallback && err.response) return fallback
        return err.message
    }
    return err instanceof Error ? err.message : 'Unknown error'
}

/** Friendly fallback shown when a bean delete is rejected (most likely: referenced by brews). */
export const BEAN_IN_USE_MESSAGE =
    "This bean can't be deleted because it's used in one or more brew entries. Delete those brews first."
