import { useAdminBrewStats } from '../hooks/useBrewStats'
import { StatsView } from './StatsView'

export function AdminAnalyticsComponent() {
    const { stats, loading, error } = useAdminBrewStats()
    return (
        <StatsView
            stats={stats}
            loading={loading}
            error={error}
            title="Global Analytics"
            subtitle="All beans and brews across every user"
            backTo={{ to: '/admin', label: 'Admin' }}
        />
    )
}
