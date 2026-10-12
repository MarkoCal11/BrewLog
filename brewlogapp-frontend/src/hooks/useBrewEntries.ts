import { useState, useEffect } from 'react'
import type { BrewEntry } from '../types/BrewEntry'
import { getAllBrewEntries } from '../api/brewEntryApi'

type UseBrewEntriesResult = {
    data: BrewEntry[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
    removeEntry: (id: number) => void
}

export function useBrewEntries(): UseBrewEntriesResult {
    const [data, setData] = useState<BrewEntry[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchEntries = () => {
        setLoading(true)
        getAllBrewEntries()
            .then(entries => {
                setData(entries)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    const removeEntry = (id: number) => {
        setData(prev => prev?.filter(entry => entry.id !== id) ?? null)
    }

    useEffect(() => { fetchEntries() }, [])

    return { data, loading, error, refetch: fetchEntries, removeEntry }
}
