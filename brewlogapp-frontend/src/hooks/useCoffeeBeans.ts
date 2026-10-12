import { useState, useEffect } from "react"
import type { CoffeeBean } from "../types/CoffeeBean"
import { getAllCoffeeBeans } from "../api/coffeeBeanApi"


type UseCoffeeBeansResult = {
    data: CoffeeBean[] | null
    loading: boolean
    error: Error | null
    refetch: () => void
    removeBean: (id: number) => void
    replaceBean: (bean: CoffeeBean) => void
}

export function useCoffeeBeans(): UseCoffeeBeansResult {
    const [data, setData] = useState<CoffeeBean[] | null>(null)
    const [loading, setLoading] = useState<boolean>(true)
    const [error, setError] = useState<Error | null>(null)

    const fetchBeans = () => {
        setLoading(true)
        getAllCoffeeBeans()
        .then(beans => {
            setData(beans)
            setLoading(false)
        })
        .catch(err => {
            setError(err)
            setLoading(false)
        })
    }

    const removeBean = (id: number) => {
        setData(prev => prev?.filter(bean => bean.id !== id) ?? null)
    }

    const replaceBean = (bean: CoffeeBean) => {
        setData(prev => prev?.map(b => b.id === bean.id ? bean : b) ?? null)
    }

    useEffect(() => { fetchBeans() }, [])

    return { data, loading, error, refetch: fetchBeans, removeBean, replaceBean }
}