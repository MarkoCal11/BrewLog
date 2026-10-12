import { useBrewStats } from '../hooks/useBrewStats'
import { StatsView } from './StatsView'

export function AnalyticsComponent() {
    const { stats, loading, error } = useBrewStats()
    return (
        <StatsView
            stats={stats}
            loading={loading}
            error={error}
            title="Analytics"
            subtitle="Patterns across your beans and brews"
        />
    )
}
