/**
 * Format a date string (ISO or timestamp) or number (ms) for display: "Feb 1 2026 - 02:00pm".
 * Returns "—" if the value is missing or invalid.
 */
export function formatDateTime(value: string | number | null | undefined): string {
  if (value == null) return '—'
  const date = typeof value === 'number' ? new Date(value) : new Date(String(value).trim())
  if (Number.isNaN(date.getTime())) return '—'
  const month = date.toLocaleString('en-US', { month: 'short' })
  const day = date.getDate()
  const year = date.getFullYear()
  const hours = date.getHours()
  const h = hours % 12 || 12
  const pad = (n: number) => n.toString().padStart(2, '0')
  const ampm = hours < 12 ? 'am' : 'pm'
  const time = `${pad(h)}:${pad(date.getMinutes())}${ampm}`
  return `${month} ${day} ${year} - ${time}`
}
