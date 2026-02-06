import { DRAFT_STATUS } from '@/types/drafts'

export interface DraftStatusDisplay {
  label: string
  className: string
  icon: string
}

const STATUS_DISPLAY: Record<string, DraftStatusDisplay> = {
  [DRAFT_STATUS.REJECTED]: {
    label: 'Rejected',
    className: 'text-gray-500',
    icon: '✕',
  },
  [DRAFT_STATUS.PENDING]: {
    label: 'Review required',
    className: 'text-orange-600',
    icon: '○',
  },
  [DRAFT_STATUS.SKIPPED]: {
    label: 'Skipped',
    className: 'text-gray-500',
    icon: '—',
  },
  [DRAFT_STATUS.ACCEPTED]: {
    label: 'Accepted',
    className: 'text-green-600',
    icon: '✓',
  },
}

const DEFAULT_DISPLAY: DraftStatusDisplay = {
  label: 'Review required',
  className: 'text-orange-600',
  icon: '○',
}

/**
 * Returns label, className, and icon for a draft status string (e.g. from API).
 * Case-insensitive; unknown/empty status defaults to pending.
 */
export function getDraftStatusDisplay(status: string | undefined): DraftStatusDisplay {
  const key = (status ?? '').toLowerCase()
  const display = STATUS_DISPLAY[key]
  return display ?? DEFAULT_DISPLAY
}
