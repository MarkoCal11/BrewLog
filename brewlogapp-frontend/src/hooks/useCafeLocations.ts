import { useState, useEffect } from 'react'
import type { CafeLocation } from '../types/CafeLocation'
import { getAllCafeLocations } from '../api/cafeLocationApi'

type UseCafeLocationsResult = {
    data: CafeLocation[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
    removeCafe: (id: number) => void
    replaceCafe: (cafe: CafeLocation) => void
}

export function useCafeLocations(): UseCafeLocationsResult {
    const [data, setData] = useState<CafeLocation[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchCafes = () => {
        setLoading(true)
        getAllCafeLocations()
            .then(cafes => {
                setData(cafes)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    const removeCafe = (id: number) => {
        setData(prev => prev?.filter(cafe => cafe.id !== id) ?? null)
    }

    const replaceCafe = (cafe: CafeLocation) => {
        setData(prev => prev?.map(c => c.id === cafe.id ? cafe : c) ?? null)
    }

    useEffect(() => { fetchCafes() }, [])

    return { data, loading, error, refetch: fetchCafes, removeCafe, replaceCafe }
}
