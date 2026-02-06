import { API_BASE } from '@/constants/api'

export interface IgnoredEmailSummary {
  emailId: string
  from?: string
  subject?: string
  date?: string
  snippet?: string
}

export const ignoredEmailsQueryKey = ['emails', 'ignored'] as const

export async function fetchIgnoredEmails(): Promise<IgnoredEmailSummary[]> {
  const r = await fetch(`${API_BASE}/emails/ignored`)
  if (!r.ok) throw new Error('Failed to load ignored emails')
  return r.json()
}
