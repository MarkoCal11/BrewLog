import { useState, useEffect } from 'react'
import type { CoffeeBean } from '../types/CoffeeBean'
import { getAllCoffeeBeansGlobal } from '../api/coffeeBeanApi'

type UseAdminCoffeeBeansResult = {
    data: CoffeeBean[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
}

export function useAdminCoffeeBeans(): UseAdminCoffeeBeansResult {
    const [data, setData] = useState<CoffeeBean[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchBeans = () => {
        setLoading(true)
        getAllCoffeeBeansGlobal()
            .then(beans => {
                setData(beans)
                setLoading(false)
            })
            .catch(err => {
                setError(err)
                setLoading(false)
            })
    }

    useEffect(() => { fetchBeans() }, [])

    return { data, loading, error, refetch: fetchBeans }
}
