/** Draft status values (aligned with backend DraftStatus). */
export const DRAFT_STATUS = {
  REJECTED: 'rejected',
  PENDING: 'pending',
  SKIPPED: 'skipped',
  ACCEPTED: 'accepted',
} as const

export type DraftStatusValue = (typeof DRAFT_STATUS)[keyof typeof DRAFT_STATUS]

/** Summary item from GET /api/drafts */
export interface DraftSummary {
  emailId: string
  subject?: string
  from?: string
  draftSubject?: string
  snippet?: string
  status?: string
  generatedAt?: string
  category?: string
}

/** Email part inside draft detail */
export interface EmailPart {
  id?: string
  from?: string
  subject?: string
  body?: string
  date?: string
}

/** Draft part inside draft detail */
export interface DraftPart {
  draftText?: string
  draftSubject?: string
  status?: string
  generatedAt?: string
  category?: string
}

/** Detail from GET /api/drafts/:emailId */
export interface DraftDetail {
  email?: EmailPart
  draft?: DraftPart
}
