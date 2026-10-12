import { useState, useEffect } from 'react'
import type { FlavorNote } from '../types/FlavorNote'
import { getAllFlavorNotes } from '../api/flavorNoteApi'

type UseFlavorNotesResult = {
    data: FlavorNote[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
    removeNote: (id: number) => void
}

export function useFlavorNotes(): UseFlavorNotesResult {
    const [data, setData] = useState<FlavorNote[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchNotes = () => {
        setLoading(true)
        getAllFlavorNotes()
        .then(notes => {
            setData(notes)
            setLoading(false)
        })
        .catch(err => {
            setError(err)
            setLoading(false)
        });
    }

    const removeNote = (id: number) => {
        setData(prev => prev?.filter(note => note.id !== id) ?? null)
    }

    useEffect(() => { fetchNotes() }, [])

    return { data, loading, error, refetch: fetchNotes, removeNote }
}