import axios from 'axios'
import type { CafeLocation } from '../types/CafeLocation'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/cafeLocations`
const ADMIN_API_URL = `${BACKEND_URL}/api/admin/cafeLocations`

export interface CafeLocationCreateRequest {
    name: string
    city?: string | null
    address?: string | null
    rating?: number | null
    instagram?: string | null
    latitude?: number | null
    longitude?: number | null
}

// Partial patch (same semantics as the bean PUT): only sent fields change.
export type CafeLocationUpdateRequest = Partial<CafeLocationCreateRequest>

export const getAllCafeLocations = async (): Promise<CafeLocation[]> => {
    const response = await axios.get<CafeLocation[]>(API_URL)
    return response.data
}

export const getCafeLocation = async (id: number): Promise<CafeLocation> => {
    const response = await axios.get<CafeLocation>(`${API_URL}/${id}`)
    return response.data
}

export const createCafeLocation = async (cafe: CafeLocationCreateRequest): Promise<CafeLocation> => {
    const response = await axios.post<CafeLocation>(API_URL, cafe)
    return response.data
}

export const updateCafeLocation = async (id: number, cafe: CafeLocationUpdateRequest): Promise<CafeLocation> => {
    const response = await axios.put<CafeLocation>(`${API_URL}/${id}`, cafe)
    return response.data
}

export const deleteCafeLocation = async (id: number): Promise<void> => {
    await axios.delete(`${API_URL}/${id}`)
}

// Admin: act on any user's café (global scope, ADMIN role only). Edit + delete
// only — no admin-create, matching the bean/brew admin scope.
export const getAllCafeLocationsGlobal = async (): Promise<CafeLocation[]> => {
    const response = await axios.get<CafeLocation[]>(ADMIN_API_URL)
    return response.data
}

export const getCafeLocationGlobal = async (id: number): Promise<CafeLocation> => {
    const response = await axios.get<CafeLocation>(`${ADMIN_API_URL}/${id}`)
    return response.data
}

export const updateCafeLocationGlobal = async (id: number, cafe: CafeLocationUpdateRequest): Promise<CafeLocation> => {
    const response = await axios.put<CafeLocation>(`${ADMIN_API_URL}/${id}`, cafe)
    return response.data
}

export const deleteCafeLocationGlobal = async (id: number): Promise<void> => {
    await axios.delete(`${ADMIN_API_URL}/${id}`)
}
