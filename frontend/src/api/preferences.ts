import { API_BASE } from '@/constants/api'

export const preferencesIgnoresQueryKey = ['preferences', 'ignores'] as const
export const preferencesSignoffQueryKey = ['preferences', 'signoff'] as const

export async function fetchIgnorePhrases(): Promise<string[]> {
  const r = await fetch(`${API_BASE}/preferences/ignores`)
  if (!r.ok) throw new Error('Failed to load ignore preferences')
  return r.json()
}

export async function updateIgnorePhrases(phrases: string[]): Promise<void> {
  const r = await fetch(`${API_BASE}/preferences/ignores`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(phrases),
  })
  if (!r.ok) throw new Error('Failed to save ignore preferences')
}

export async function fetchSignoff(): Promise<string> {
  const r = await fetch(`${API_BASE}/preferences/signoff`)
  if (!r.ok) throw new Error('Failed to load sign-off preference')
  return r.json()
}

export async function updateSignoff(signoff: string): Promise<void> {
  const r = await fetch(`${API_BASE}/preferences/signoff`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(signoff),
  })
  if (!r.ok) throw new Error('Failed to save sign-off preference')
}
