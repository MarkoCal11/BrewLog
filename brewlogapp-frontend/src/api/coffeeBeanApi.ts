import axios from 'axios'
import type { CoffeeBean, ProcessingMethod, RoastLevel } from '../types/CoffeeBean'
import { BACKEND_URL } from './config'

const API_URL = `${BACKEND_URL}/api/coffeeBeans`
const ADMIN_API_URL = `${BACKEND_URL}/api/admin/coffeeBeans`

export interface CoffeeBeanCreateRequest {
    name: string
    origin: string
    roastLevel: RoastLevel
    processingMethod?: ProcessingMethod | null
    roasterName: string
    roastDate?: string | null
    price?: number | null
    weightGrams?: number | null
    altitude?: number | null
    tastingNotes?: string | null
}

export interface CoffeeBeanUpdateRequest {
    name?: string
    origin?: string
    roastLevel?: RoastLevel
    processingMethod?: ProcessingMethod | null
    roasterName?: string
    roastDate?: string | null
    price?: number | null
    weightGrams?: number | null
    altitude?: number | null
    tastingNotes?: string | null
    // NOTE: favorite/finished are deliberately absent — they're set only via the
    // dedicated PATCH /{id}/favorite and /{id}/finished toggles, never the PUT.
}

export const getAllCoffeeBeans = async(): Promise<CoffeeBean[]> => {
    const response = await axios.get<CoffeeBean[]>(API_URL)
    return response.data
}

export const getAllCoffeeBeansGlobal = async(): Promise<CoffeeBean[]> => {
    const response = await axios.get<CoffeeBean[]>(ADMIN_API_URL)
    return response.data
}

export const getCoffeeBean = async(id:number): Promise<CoffeeBean> => {
    const response = await axios.get<CoffeeBean>(`${API_URL}/${id}`)
    return response.data
}

export const createCoffeeBean = async (bean: CoffeeBeanCreateRequest): Promise<CoffeeBean> => {
    const response = await axios.post<CoffeeBean>(API_URL, bean)
    return response.data
}

export const updateCoffeeBean = async (id: number, bean: CoffeeBeanUpdateRequest): Promise<CoffeeBean> => {
    const response = await axios.put<CoffeeBean>(`${API_URL}/${id}`, bean)
    return response.data
}

// Toggle-only endpoints: favorite/finished are one-click states, not form submissions, so
// they PATCH a single flag instead of round-tripping the whole entity through the
// full-replace PUT (which would arrive with every other field null). Backend: PATCH
// /{id}/favorite?value= and /{id}/finished?value=.
export const setBeanFavorite = async (id: number, value: boolean): Promise<CoffeeBean> => {
    const response = await axios.patch<CoffeeBean>(`${API_URL}/${id}/favorite`, null, { params: { value } })
    return response.data
}

export const setBeanFinished = async (id: number, value: boolean): Promise<CoffeeBean> => {
    const response = await axios.patch<CoffeeBean>(`${API_URL}/${id}/finished`, null, { params: { value } })
    return response.data
}

export const deleteCoffeeBean = async (id: number): Promise<void> => {
    await axios.delete(`${API_URL}/${id}`)
}

// Admin: act on any user's bean (global scope, ADMIN role only)
export const getCoffeeBeanGlobal = async (id: number): Promise<CoffeeBean> => {
    const response = await axios.get<CoffeeBean>(`${ADMIN_API_URL}/${id}`)
    return response.data
}

export const updateCoffeeBeanGlobal = async (id: number, bean: CoffeeBeanUpdateRequest): Promise<CoffeeBean> => {
    const response = await axios.put<CoffeeBean>(`${ADMIN_API_URL}/${id}`, bean)
    return response.data
}

export const deleteCoffeeBeanGlobal = async (id: number): Promise<void> => {
    await axios.delete(`${ADMIN_API_URL}/${id}`)
}

// Admin: remove any user's bean photo (moderation). No admin upload/change by design —
// admin scope is edit + delete only. Deletes the file + nulls photoUrl on the bean.
export const deleteBeanPhotoGlobal = async (id: number): Promise<void> => {
    await axios.delete(`${ADMIN_API_URL}/${id}/photo`)
}

// Photo: multipart upload to file-on-disk storage; backend stores a relative photoUrl
// on the bean and returns the updated bean. Owner-scoped like the other bean writes.
export const uploadBeanPhoto = async (id: number, file: Blob): Promise<CoffeeBean> => {
    const formData = new FormData()
    formData.append('file', file, file instanceof File ? file.name : 'photo')
    const response = await axios.post<CoffeeBean>(`${API_URL}/${id}/photo`, formData)
    return response.data
}

export const deleteBeanPhoto = async (id: number): Promise<void> => {
    await axios.delete(`${API_URL}/${id}/photo`)
}

export const addFlavorNoteToBean = async (beanId: number, flavorNoteId: number): Promise<void> => {
    await axios.post(`${API_URL}/${beanId}/flavorNotes/${flavorNoteId}`)
}

export const removeFlavorNoteFromBean = async (beanId: number, flavorNoteId: number): Promise<void> => {
    await axios.delete(`${API_URL}/${beanId}/flavorNotes/${flavorNoteId}`)
}