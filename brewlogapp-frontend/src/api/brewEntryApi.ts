import axios from 'axios'
import type { BrewEntry, Method } from '../types/BrewEntry'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/brewEntries`
const ADMIN_API_URL = `${BACKEND_URL}/api/admin/brewEntries`

export interface BrewEntryCreateRequest {
    method: Method
    grindSize?: number | null
    waterTempC?: number | null
    brewTimeSeconds?: number | null
    doseGrams: number
    brewedAt: string
    coffeeBeanId: number
    waterGrams?: number | null
    yieldGrams?: number | null
    rating?: number | null
    notes?: string | null
}

export interface BrewEntryUpdateRequest {
    method?: Method
    grindSize?: number | null
    waterTempC?: number | null
    brewTimeSeconds?: number | null
    doseGrams?: number
    waterGrams?: number | null
    yieldGrams?: number | null
    rating?: number | null
    notes?: string | null
    brewedAt?: string
    coffeeBeanId?: number
}

export const getAllBrewEntries = async (): Promise<BrewEntry[]> => {
    const response = await axios.get<BrewEntry[]>(API_URL)
    return response.data
}

export const getAllBrewEntriesGlobal = async (): Promise<BrewEntry[]> => {
    const response = await axios.get<BrewEntry[]>(ADMIN_API_URL)
    return response.data
}

export const getBrewEntry = async (id: number): Promise<BrewEntry> => {
    const response = await axios.get<BrewEntry>(`${API_URL}/${id}`)
    return response.data
}

export const createBrewEntry = async (entry: BrewEntryCreateRequest): Promise<BrewEntry> => {
    const response = await axios.post<BrewEntry>(API_URL, entry)
    return response.data
}

export const updateBrewEntry = async (id: number, entry: BrewEntryUpdateRequest): Promise<BrewEntry> => {
    const response = await axios.put<BrewEntry>(`${API_URL}/${id}`, entry)
    return response.data
}

export const deleteBrewEntry = async (id: number): Promise<void> => {
    await axios.delete(`${API_URL}/${id}`)
}

// Admin: act on any user's brew (global scope, ADMIN role only)
export const getBrewEntryGlobal = async (id: number): Promise<BrewEntry> => {
    const response = await axios.get<BrewEntry>(`${ADMIN_API_URL}/${id}`)
    return response.data
}

export const updateBrewEntryGlobal = async (id: number, entry: BrewEntryUpdateRequest): Promise<BrewEntry> => {
    const response = await axios.put<BrewEntry>(`${ADMIN_API_URL}/${id}`, entry)
    return response.data
}

export const deleteBrewEntryGlobal = async (id: number): Promise<void> => {
    await axios.delete(`${ADMIN_API_URL}/${id}`)
}
