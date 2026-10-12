import {
    ResponsiveContainer,
    RadarChart,
    PolarGrid,
    PolarAngleAxis,
    Radar,
} from 'recharts'

type RadarPoint = { category: string; count: number }

type BeanFlavorRadarProps = {
    /** One point per flavor-note category, value = how many of the bean's notes fall in it. */
    data: RadarPoint[]
}

/**
 * A bean's tasting profile as a radar. Lazy-loaded so recharts stays out of
 * the main bundle (shared with the analytics chunk).
 */
export function BeanFlavorRadar({ data }: BeanFlavorRadarProps) {
    return (
        <div className="chart-box">
            <ResponsiveContainer width="100%" height={260}>
                <RadarChart data={data} margin={{ top: 8, right: 24, bottom: 8, left: 24 }}>
                    <PolarGrid stroke="var(--border)" />
                    <PolarAngleAxis dataKey="category" tick={{ fill: 'var(--text-tertiary)', fontSize: 12 }} />
                    <Radar
                        dataKey="count"
                        stroke="var(--accent)"
                        fill="var(--accent)"
                        fillOpacity={0.35}
                    />
                </RadarChart>
            </ResponsiveContainer>
        </div>
    )
}
