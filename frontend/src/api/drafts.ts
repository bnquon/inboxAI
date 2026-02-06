import { API_BASE } from '@/constants/api'
import type { DraftSummary } from '@/types/drafts'

export const draftsQueryKey = ['drafts'] as const

export async function fetchDrafts(): Promise<DraftSummary[]> {
  const r = await fetch(`${API_BASE}/drafts`)
  if (!r.ok) throw new Error('Failed to load drafts')
  return r.json()
}

export async function rejectDraft(emailId: string): Promise<void> {
  const r = await fetch(`${API_BASE}/drafts/${encodeURIComponent(emailId)}/reject`, {
    method: 'PATCH',
  })
  if (!r.ok) throw new Error(r.status === 404 ? 'Draft not found' : 'Failed to reject draft')
}

export async function skipDraft(emailId: string): Promise<void> {
  const r = await fetch(`${API_BASE}/drafts/${encodeURIComponent(emailId)}/skip`, {
    method: 'PATCH',
  })
  if (!r.ok) throw new Error(r.status === 404 ? 'Draft not found' : 'Failed to skip draft')
}

export interface UpdateDraftBody {
  draftText?: string
  draftSubject?: string
}

export async function updateDraft(emailId: string, body: UpdateDraftBody): Promise<void> {
  const r = await fetch(`${API_BASE}/drafts/${encodeURIComponent(emailId)}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!r.ok) throw new Error(r.status === 404 ? 'Draft not found' : 'Failed to update draft')
}

export async function sendDraft(emailId: string): Promise<void> {
  const r = await fetch(`${API_BASE}/drafts/${encodeURIComponent(emailId)}/send`, {
    method: 'POST',
  })
  if (!r.ok) {
    const message = await r.text().catch(() => r.statusText)
    throw new Error(r.status === 404 ? 'Draft not found' : message || 'Failed to send email')
  }
}
