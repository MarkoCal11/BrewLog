import { useState, useEffect } from 'react'
import type { AdminUser } from '../types/Auth'
import { getAllUsers } from '../api/adminUserApi'

type UseAdminUsersResult = {
    data: AdminUser[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
    replaceUser: (user: AdminUser) => void
}

export function useAdminUsers(): UseAdminUsersResult {
    const [data, setData] = useState<AdminUser[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchUsers = () => {
        setLoading(true)
        getAllUsers()
            .then(users => {
                setData(users)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    const replaceUser = (user: AdminUser) => {
        setData(prev => prev?.map(u => u.id === user.id ? user : u) ?? null)
    }

    useEffect(() => { fetchUsers() }, [])

    return { data, loading, error, refetch: fetchUsers, replaceUser }
}
