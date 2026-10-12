import { fetchEventSource } from '@microsoft/fetch-event-source'
import { getAccessToken, refresh } from './authApi'
import { BACKEND_URL } from './config'

// Real-time "a new report was generated" push for the logged-in user. The backend
// Quartz job emits an SSE event on the user's stream the moment it persists a new
// report; the frontend reacts by refetching the reports list so the home banner and
// reports page update live — replacing the old poll-on-load behaviour.
//
// Why fetch-based SSE (not native EventSource): the access token is kept in memory
// only (never localStorage/URL), and native EventSource can't send an Authorization
// header. fetch-based SSE carries the Bearer token as a header AND lets us run the
// same 401 -> silent-refresh -> reconnect dance as the axios interceptor.
//
// BACKEND CONTRACT (separate Spring repo — not yet built):
//   GET {BACKEND_URL}/api/reports/stream     Accept: text/event-stream
//     - Behind the normal JWT filter (Authorization: Bearer <access token>).
//     - Scoped to the authenticated user's own reports.
//     - Emits a named `report` event whenever a new report is persisted. The data may
//       be the Report JSON or just its id; the client only uses it as a "refetch now"
//       cue today. Send a heartbeat comment (`:\n\n`) every ~25s so intermediary
//       proxies don't idle-close the stream.

const STREAM_URL = `${BACKEND_URL}/api/reports/stream`

type Listener = () => void
const listeners = new Set<Listener>()
let controller: AbortController | null = null

// A 4xx other than 401 (e.g. 404 before the backend endpoint ships, or 403) is fatal:
// retrying can't help, so we stop and silently fall back to poll-on-load.
class FatalStreamError extends Error {}

function notify(): void {
    for (const listener of listeners) listener()
}

function start(): void {
    if (controller) return
    controller = new AbortController()

    fetchEventSource(STREAM_URL, {
        signal: controller.signal,
        // Keep streaming even when the tab is backgrounded — a report can land while
        // the user is away, and we want the banner ready the instant they return.
        openWhenHidden: true,
        // Custom fetch so each (re)connect reads the *current* in-memory token and
        // carries the refresh cookie. fetchEventSource invokes this on every retry, so
        // a token refreshed mid-session is picked up automatically on reconnect.
        fetch: (input, init) => {
            const token = getAccessToken()
            return fetch(input, {
                ...init,
                credentials: 'include',
                headers: {
                    ...init?.headers,
                    ...(token ? { Authorization: `Bearer ${token}` } : {}),
                },
            })
        },
        async onopen(res) {
            const contentType = res.headers.get('content-type') ?? ''
            if (res.ok && contentType.includes('text/event-stream')) return
            if (res.status === 401) {
                // Access token expired: refresh once, then throw to force a reconnect
                // (which re-reads the new token via the fetch wrapper above). If the
                // refresh itself fails the user is effectively logged out -> fatal.
                try {
                    await refresh()
                } catch {
                    throw new FatalStreamError('refresh failed')
                }
                throw new Error('retry-after-refresh')
            }
            // 404 (endpoint not deployed yet), 403, etc. -> don't hammer the server.
            throw new FatalStreamError(`stream open failed: ${res.status}`)
        },
        onmessage(ev) {
            // Ignore heartbeats / unnamed comments; any real report event is a refetch cue.
            if (ev.event === 'ping' || !ev.data) return
            notify()
        },
        onerror(err) {
            // Fatal -> rethrow to abort the stream for good. Transient (network blip,
            // post-refresh reconnect) -> return a backoff delay (ms) to reconnect.
            if (err instanceof FatalStreamError) throw err
            return 3000
        },
    }).catch(() => {
        // A fatal abort rejects the promise here; clear so a later subscribe can retry.
        controller = null
    })
}

function stop(): void {
    controller?.abort()
    controller = null
}

// Subscribe to "a new report arrived" pushes. The underlying SSE connection is a
// singleton: it opens on the first subscriber and closes when the last unsubscribes,
// so any number of report consumers share a single stream. Returns an unsubscribe fn.
export function subscribeReportEvents(listener: Listener): () => void {
    listeners.add(listener)
    if (listeners.size === 1) start()
    return () => {
        listeners.delete(listener)
        if (listeners.size === 0) stop()
    }
}
