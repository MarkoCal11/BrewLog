import axios from 'axios'
import type { Report } from '../types/Report'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/reports`
const ADMIN_API_URL = `${BACKEND_URL}/api/admin/reports`

// Reports are generated and persisted by the backend Quartz job — the frontend only
// reads them, so there is no create/update/delete here.

export const getAllReports = async (): Promise<Report[]> => {
    const response = await axios.get<Report[]>(API_URL)
    return response.data
}

export const getReport = async (id: number): Promise<Report> => {
    const response = await axios.get<Report>(`${API_URL}/${id}`)
    return response.data
}

// Admin: system-wide aggregate reports rolling up ALL users — one row per period
// (weekly/monthly), each with `activeUsers` and no per-user owner. ADMIN role only,
// enforced server-side by the /api/admin/* endpoints. Per the privacy stance, the
// admin sees platform aggregates, never an individual user's report.
export const getAllReportsGlobal = async (): Promise<Report[]> => {
    const response = await axios.get<Report[]>(ADMIN_API_URL)
    return response.data
}

export const getReportGlobal = async (id: number): Promise<Report> => {
    const response = await axios.get<Report>(`${ADMIN_API_URL}/${id}`)
    return response.data
}
