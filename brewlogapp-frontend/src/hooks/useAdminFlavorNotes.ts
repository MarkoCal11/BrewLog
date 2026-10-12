import { useState, useEffect } from 'react'
import type { FlavorNote } from '../types/FlavorNote'
import { getGlobalFlavorNotes } from '../api/flavorNoteApi'

type UseAdminFlavorNotesResult = {
    data: FlavorNote[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useAdminFlavorNotes(): UseAdminFlavorNotesResult {
    const [data, setData] = useState<FlavorNote[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchNotes = () => {
        setLoading(true)
        getGlobalFlavorNotes()
            .then(notes => {
                setData(notes)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    useEffect(() => { fetchNotes() }, [])

    return { data, loading, error, refetch: fetchNotes }
}
