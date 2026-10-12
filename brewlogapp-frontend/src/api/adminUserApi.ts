import axios from 'axios'
import type { AdminUser, AdminUserStats, Role } from '../types/Auth'
import { BACKEND_URL } from './config'

const ADMIN_API_URL = `${BACKEND_URL}/api/admin/users`

export const getAllUsers = async (): Promise<AdminUser[]> => {
    const response = await axios.get<AdminUser[]>(ADMIN_API_URL)
    return response.data
}

// Single account, so the detail page deep-links/refreshes without fetching the
// whole list. 404 (caught upstream) if the id doesn't exist.
export const getUser = async (id: number): Promise<AdminUser> => {
    const response = await axios.get<AdminUser>(`${ADMIN_API_URL}/${id}`)
    return response.data
}

// Aggregate counts tallied in SQL server-side (no entity loading). 404 if the
// user doesn't exist — never a misleading {0,0,0}.
export const getUserStats = async (id: number): Promise<AdminUserStats> => {
    const response = await axios.get<AdminUserStats>(`${ADMIN_API_URL}/${id}/stats`)
    return response.data
}

// Soft delete: deactivate/reactivate flip the user's `active` flag (reversible,
// no orphaned data). Both return the updated user. Last-active-admin guard is
// enforced server-side; the UI mirrors it.
export const deactivateUser = async (id: number): Promise<AdminUser> => {
    const response = await axios.post<AdminUser>(`${ADMIN_API_URL}/${id}/deactivate`)
    return response.data
}

export const reactivateUser = async (id: number): Promise<AdminUser> => {
    const response = await axios.post<AdminUser>(`${ADMIN_API_URL}/${id}/reactivate`)
    return response.data
}

// Role change: PUT /{id}/role with { role }. Returns the updated user.
// Last-active-admin guard is enforced server-side; the UI mirrors it.
export const updateUserRole = async (id: number, role: Role): Promise<AdminUser> => {
    const response = await axios.put<AdminUser>(`${ADMIN_API_URL}/${id}/role`, { role })
    return response.data
}
