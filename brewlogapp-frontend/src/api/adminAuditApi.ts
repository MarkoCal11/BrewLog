import axios from 'axios'
import type { AuditLog, Page } from '../types/AuditLog'
import { BACKEND_URL } from './config'

const ADMIN_API_URL = `${BACKEND_URL}/api/admin/audit-logs`

// Read-only audit trail of admin actions against accounts (newest-first, paginated).
// Pass targetUserId to scope the trail to a single account; omit it for the
// app-wide log (the param is optional server-side, so unfiltered calls are unchanged).
export const getAuditLogs = async (
    page = 0,
    size = 20,
    targetUserId?: number,
): Promise<Page<AuditLog>> => {
    const response = await axios.get<Page<AuditLog>>(ADMIN_API_URL, {
        params: { page, size, targetUserId },
    })
    return response.data
}
