import { useState } from 'react'
import { Link } from 'react-router'
import { useCoffeeBeans } from '../hooks/useCoffeeBeans'
import { useBrewEntries } from '../hooks/useBrewEntries'
import { useFlavorNotes } from '../hooks/useFlavorNotes'
import { useCafeLocations } from '../hooks/useCafeLocations'
import {
    todayStamp,
    downloadFile,
    beansToCsv,
    brewsToCsv,
    flavorNotesToCsv,
    cafesToCsv,
    bundleToJson,
} from '../utils/dataExport'

export function ExportComponent() {
    const { data: beans, loading: beansLoading } = useCoffeeBeans()
    const { data: brews, loading: brewsLoading } = useBrewEntries()
    const { data: notes, loading: notesLoading } = useFlavorNotes()
    const { data: cafes, loading: cafesLoading } = useCafeLocations()

    const loading = beansLoading || brewsLoading || notesLoading || cafesLoading

    const beanList = beans ?? []
    const brewList = brews ?? []
    const noteList = notes ?? []
    const cafeList = cafes ?? []

    // Native export can fail (filesystem/share); surface it instead of failing silently.
    const [exportError, setExportError] = useState<string | null>(null)

    const runExport = async (action: () => Promise<void>) => {
        setExportError(null)
        try {
            await action()
        } catch (err) {
            setExportError(err instanceof Error ? err.message : 'Could not export your data.')
        }
    }

    const downloadJson = () => runExport(() =>
        downloadFile(
            `brewlog-export-${todayStamp()}.json`,
            bundleToJson({ beans: beanList, brews: brewList, flavorNotes: noteList, cafes: cafeList }),
            'application/json',
        ),
    )

    const csvDownloads = [
        { label: 'Beans', count: beanList.length, make: () => beansToCsv(beanList), file: 'beans' },
        { label: 'Brews', count: brewList.length, make: () => brewsToCsv(brewList), file: 'brews' },
        { label: 'Flavor notes', count: noteList.length, make: () => flavorNotesToCsv(noteList), file: 'flavor-notes' },
        { label: 'Cafés', count: cafeList.length, make: () => cafesToCsv(cafeList), file: 'cafes' },
    ]

    const downloadCsv = (make: () => string, file: string) => runExport(() =>
        downloadFile(`brewlog-${file}-${todayStamp()}.csv`, make(), 'text/csv'),
    )

    const total = beanList.length + brewList.length + noteList.length + cafeList.length

    return (
        <div className="stack-lg">
            <Link to="/" className="back-link">Home</Link>
            <header className="page-header">
                <h1 className="page-title">Export your data</h1>
                <p className="page-subtitle">Download a copy of everything you've logged. Everything happens on your device - nothing is uploaded.</p>
            </header>

            {loading ? (
                <div className="empty">Loading your data…</div>
            ) : (
                <>
                    <div className="card">
                        <div className="card-title">Full backup (JSON)</div>
                        <div className="card-meta" style={{ marginBottom: 12 }}>
                            One file with all {total} records, keeping every detail and relationship - ideal as a backup.
                        </div>
                        <button className="btn btn-primary" onClick={downloadJson} disabled={total === 0}>
                            Download backup (.json)
                        </button>
                    </div>

                    <div className="card">
                        <div className="card-title">Spreadsheets (CSV)</div>
                        <div className="card-meta" style={{ marginBottom: 12 }}>
                            One file per dataset, ready to open in Excel or Google Sheets.
                        </div>
                        <div className="home-actions">
                            {csvDownloads.map(d => (
                                <button
                                    key={d.file}
                                    className="btn btn-ghost"
                                    onClick={() => downloadCsv(d.make, d.file)}
                                    disabled={d.count === 0}
                                >
                                    {d.label} ({d.count})
                                </button>
                            ))}
                        </div>
                    </div>

                    {exportError && <div className="error-inline">{exportError}</div>}
                </>
            )}
        </div>
    )
}

