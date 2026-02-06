import { formatDateTime } from '@/utils/date'
import { getDraftStatusDisplay } from '@/utils/draftStatus'
import type { DraftSummary } from '@/types/drafts'
import { DRAFT_STATUS } from '@/types/drafts'

export interface DraftCardProps {
  draft: DraftSummary
  onClick: () => void
  onSkip?: () => void
}

export function DraftCard({ draft, onClick, onSkip }: DraftCardProps) {
  const statusDisplay = getDraftStatusDisplay(draft.status)
  const status = (draft.status ?? '').toLowerCase()
  const canSkip = onSkip && status !== DRAFT_STATUS.SKIPPED && status !== DRAFT_STATUS.ACCEPTED && status !== DRAFT_STATUS.REJECTED

  return (
    <li>
      <div className="flex items-stretch rounded-lg border border-gray-200 bg-white overflow-hidden">
        <button
          type="button"
          onClick={onClick}
          className="flex-1 min-w-0 cursor-pointer text-left p-4 transition-colors hover:bg-gray-50"
        >
          <div className="flex items-center gap-2 flex-wrap">
            {draft.category && (
              <span className="text-xs font-medium px-2 py-0.5 rounded bg-gray-200 text-gray-700">
                {draft.category}
              </span>
            )}
            <span className="text-xs text-gray-400">{formatDateTime(draft.generatedAt)}</span>
          </div>
          <p className="font-medium text-gray-800 mt-1 truncate">{draft.from}</p>
          <p className="text-sm text-gray-600 truncate">{draft.subject}</p>
          {draft.snippet && (
            <p className="text-sm text-gray-500 truncate mt-1">{draft.snippet}</p>
          )}
          <p className={`text-xs mt-2 flex items-center gap-1.5 ${statusDisplay.className}`}>
            <span aria-hidden>{statusDisplay.icon}</span>
            {statusDisplay.label}
          </p>
        </button>
        {canSkip && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation()
              onSkip?.()
            }}
            className="shrink-0 px-4 border-l border-gray-200 text-sm font-medium text-gray-600 hover:bg-gray-100 hover:text-gray-800 transition-colors"
            title="Skip this draft"
          >
            Skip
          </button>
        )}
      </div>
    </li>
  )
}
