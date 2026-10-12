import { useState, useEffect } from 'react'
import type { BrewEntry } from '../types/BrewEntry'
import { getAllBrewEntriesGlobal } from '../api/brewEntryApi'

type UseAdminBrewEntriesResult = {
    data: BrewEntry[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useAdminBrewEntries(): UseAdminBrewEntriesResult {
    const [data, setData] = useState<BrewEntry[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchEntries = () => {
        setLoading(true)
        getAllBrewEntriesGlobal()
            .then(entries => {
                setData(entries)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    useEffect(() => { fetchEntries() }, [])

    return { data, loading, error, refetch: fetchEntries }
}
