import { useState, useEffect } from 'react'
import type { CafeLocation } from '../types/CafeLocation'
import { getAllCafeLocationsGlobal } from '../api/cafeLocationApi'

type UseAdminCafeLocationsResult = {
    data: CafeLocation[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useAdminCafeLocations(): UseAdminCafeLocationsResult {
    const [data, setData] = useState<CafeLocation[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchCafes = () => {
        setLoading(true)
        getAllCafeLocationsGlobal()
            .then(cafes => {
                setData(cafes)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    useEffect(() => { fetchCafes() }, [])

    return { data, loading, error, refetch: fetchCafes }
}
