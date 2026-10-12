import type { CoffeeBean } from '../types/CoffeeBean'
import type { BrewEntry } from '../types/BrewEntry'
import type { FlavorNote } from '../types/FlavorNote'
import { Capacitor } from '@capacitor/core'
import type { CafeLocation } from '../types/CafeLocation'
import { instagramLabel } from '../types/CafeLocation'

/** Today as YYYY-MM-DD, for filenames. */
export function todayStamp(): string {
    return new Date().toISOString().slice(0, 10)
}

/**
 * Save `content` as a file the user can keep. On the web this triggers a normal browser
 * download; on native (iOS/Android) the WKWebView ignores `<a download>`, so we write the
 * file to the app's cache dir and hand it to the OS share sheet (Save to Files, AirDrop,
 * Mail, …). Returns once the download starts / the share sheet has been handled.
 */
export async function downloadFile(filename: string, content: string, mime: string): Promise<void> {
    if (Capacitor.isNativePlatform()) {
        await saveNative(filename, content)
        return
    }
    const blob = new Blob([content], { type: `${mime};charset=utf-8` })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
}

/**
 * Native save path. Plugins are dynamically imported so the web bundle never pulls them
 * in (mirrors how authApi loads @capacitor/preferences). The user dismissing the share
 * sheet is a normal outcome, not an error, so we swallow that one case.
 */
async function saveNative(filename: string, content: string): Promise<void> {
    const { Filesystem, Directory, Encoding } = await import('@capacitor/filesystem')
    const { Share } = await import('@capacitor/share')

    await Filesystem.writeFile({
        path: filename,
        data: content,
        directory: Directory.Cache,
        encoding: Encoding.UTF8,
    })
    const { uri } = await Filesystem.getUri({ path: filename, directory: Directory.Cache })

    try {
        await Share.share({ title: filename, url: uri, dialogTitle: 'Export your BrewLog data' })
    } catch (err) {
        // The OS share sheet throws when the user cancels it — not worth surfacing.
        if (String((err as { message?: string })?.message ?? '').toLowerCase().includes('cancel')) return
        throw err
    }
}

/**
 * Output is fixed to the European spreadsheet convention: ';' field delimiter and a
 * comma decimal separator. Browser-locale auto-detection proved unreliable (a US-locale
 * browser would emit dot decimals that a European Excel reads as dates — e.g. "3.5"
 * becomes 3 May — and would dump rows into one cell), so we commit to the format that
 * matches the Excel these files are opened in. Every numeric cell then parses as a
 * number and right-aligns instead of landing as left-aligned text or a bogus date.
 */
const DELIMITER = ';'
const NEEDS_QUOTE = /["\n\r;]/

/** Render a number with a comma decimal separator and no thousands grouping. */
function formatNumber(n: number): string {
    if (!Number.isFinite(n)) return ''
    return String(n).replace('.', ',') // String(n) is canonical: dot decimal, no grouping
}

/**
 * Render one CSV cell. Numbers use a comma decimal so the spreadsheet treats them as
 * numbers; a cell is quoted only when it contains the ';' delimiter, a quote, or a
 * newline — importantly we never quote the comma in a decimal like "4,5", or Excel
 * would read it back as text.
 */
function csvCell(value: unknown): string {
    if (value == null) return ''
    const s = typeof value === 'number' ? formatNumber(value) : String(value)
    return NEEDS_QUOTE.test(s) ? `"${s.replace(/"/g, '""')}"` : s
}

type Column<T> = { header: string; value: (row: T) => unknown }

/** Build a CSV string (header row + one row per record) from typed columns. */
function toCsv<T>(rows: T[], columns: Column<T>[]): string {
    const head = columns.map(c => csvCell(c.header)).join(DELIMITER)
    const body = rows.map(row => columns.map(c => csvCell(c.value(row))).join(DELIMITER))
    // Excel ignores the file's delimiter on double-click and uses the OS "List separator"
    // instead, so a locale mismatch dumps the whole row into one cell. A leading `sep=`
    // line tells Excel exactly which delimiter to use. (Google Sheets ignores it — it just
    // shows up as a throwaway first row there.)
    return [`sep=${DELIMITER}`, head, ...body].join('\r\n')
}

export function beansToCsv(beans: CoffeeBean[]): string {
    return toCsv(beans, [
        { header: 'id', value: b => b.id },
        { header: 'name', value: b => b.name },
        { header: 'origin', value: b => b.origin },
        { header: 'roastLevel', value: b => b.roastLevel },
        { header: 'processingMethod', value: b => b.processingMethod },
        { header: 'roaster', value: b => b.roasterName },
        { header: 'averageRating', value: b => b.averageRating },
        { header: 'brewCount', value: b => b.brewCount },
        { header: 'roastDate', value: b => b.roastDate },
        { header: 'price', value: b => b.price },
        { header: 'weightGrams', value: b => b.weightGrams },
        { header: 'altitude', value: b => b.altitude },
        { header: 'favorite', value: b => b.favorite },
        { header: 'finished', value: b => b.finished ?? false },
        { header: 'flavorNotes', value: b => (b.flavorNotes ?? []).map(n => n.name).join('; ') },
        { header: 'tastingNotes', value: b => b.tastingNotes },
    ])
}

export function brewsToCsv(brews: BrewEntry[]): string {
    return toCsv(brews, [
        { header: 'id', value: b => b.id },
        { header: 'brewedAt', value: b => b.brewedAt },
        { header: 'beanId', value: b => b.coffeeBean?.id },
        { header: 'beanName', value: b => b.coffeeBean?.name },
        { header: 'method', value: b => b.method },
        { header: 'doseGrams', value: b => b.doseGrams },
        { header: 'waterGrams', value: b => b.waterGrams },
        { header: 'yieldGrams', value: b => b.yieldGrams },
        { header: 'brewTimeSeconds', value: b => b.brewTimeSeconds },
        { header: 'grindSize', value: b => b.grindSize },
        { header: 'waterTempC', value: b => b.waterTempC },
        { header: 'rating', value: b => b.rating },
        { header: 'notes', value: b => b.notes },
    ])
}

export function flavorNotesToCsv(notes: FlavorNote[]): string {
    return toCsv(notes, [
        { header: 'id', value: n => n.id },
        { header: 'name', value: n => n.name },
        { header: 'category', value: n => n.category },
        { header: 'global', value: n => n.global ?? false },
    ])
}

export function cafesToCsv(cafes: CafeLocation[]): string {
    return toCsv(cafes, [
        { header: 'id', value: c => c.id },
        { header: 'name', value: c => c.name },
        { header: 'city', value: c => c.city },
        { header: 'address', value: c => c.address },
        { header: 'rating', value: c => c.rating },
        { header: 'instagram', value: c => (c.instagram ? instagramLabel(c.instagram) : null) },
        { header: 'latitude', value: c => c.latitude },
        { header: 'longitude', value: c => c.longitude },
    ])
}

export type ExportBundle = {
    beans: CoffeeBean[]
    brews: BrewEntry[]
    flavorNotes: FlavorNote[]
    cafes: CafeLocation[]
}

/** Full-fidelity JSON backup of everything, including nested relationships. */
export function bundleToJson(bundle: ExportBundle): string {
    return JSON.stringify(
        {
            app: 'BrewLog',
            exportedAt: new Date().toISOString(),
            ...bundle,
        },
        null,
        2,
    )
}
