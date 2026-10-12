import axios from 'axios'
import type { FlavorNote } from '../types/FlavorNote'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/flavorNotes`
const ADMIN_API_URL = `${BACKEND_URL}/api/admin/flavorNotes`

export interface FlavorNoteCreateRequest {
    name: string
    category: string
}

export interface FlavorNoteUpdateRequest {
    name?: string
    category?: string
}

export const getAllFlavorNotes = async(): Promise<FlavorNote[]> => {
    const response = await axios.get<FlavorNote[]>(API_URL)
    return response.data
}

export const getFlavorNote = async(id:number): Promise<FlavorNote> => {
    const response = await axios.get<FlavorNote>(`${API_URL}/${id}`)
    return response.data
}

export const getFlavorNotesForBean = async (beanId: number): Promise<FlavorNote[]> => {
    const response = await axios.get<FlavorNote[]>(`${API_URL}/coffeeBean/${beanId}`)
    return response.data
}

export const createFlavorNote = async (note: FlavorNoteCreateRequest): Promise<FlavorNote> => {
    const response = await axios.post<FlavorNote>(API_URL, note)
    return response.data
}

export const updateFlavorNote = async (id: number, note: FlavorNoteUpdateRequest): Promise<FlavorNote> => {
    const response = await axios.put<FlavorNote>(`${API_URL}/${id}`, note)
    return response.data
}

export const deleteFlavorNote = async (id: number): Promise<void> => {
    await axios.delete(`${API_URL}/${id}`)
}

// Admin-only: manage the shared (global, user_id IS NULL) flavor notes.
// Authorization is enforced server-side by the /api/admin/* endpoints.
export const getGlobalFlavorNotes = async (): Promise<FlavorNote[]> => {
    const response = await axios.get<FlavorNote[]>(ADMIN_API_URL)
    return response.data
}

export const createGlobalFlavorNote = async (note: FlavorNoteCreateRequest): Promise<FlavorNote> => {
    const response = await axios.post<FlavorNote>(ADMIN_API_URL, note)
    return response.data
}

export const updateGlobalFlavorNote = async (id: number, note: FlavorNoteUpdateRequest): Promise<FlavorNote> => {
    const response = await axios.put<FlavorNote>(`${ADMIN_API_URL}/${id}`, note)
    return response.data
}

export const deleteGlobalFlavorNote = async (id: number): Promise<void> => {
    await axios.delete(`${ADMIN_API_URL}/${id}`)
}