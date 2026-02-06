import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { DraftCard } from '@/components/DraftCard'
import { draftsQueryKey, fetchDrafts, skipDraft } from '@/api/drafts'
import { ignoredEmailsQueryKey, fetchIgnoredEmails } from '@/api/emails'
import { DRAFT_STATUS, type DraftSummary } from '@/types/drafts'
import { formatDateTime } from '@/utils/date'

const FILTER_TABS: string[] = ['pending', 'all', 'accepted', 'rejected', 'ignored']
const FILTER_TAB_LABELS: Record<string, string> = {
  all: 'All',
  pending: 'Review pending',
  accepted: 'Accepted',
  rejected: 'Rejected',
  ignored: 'Ignored',
}

function filterDrafts(drafts: DraftSummary[], tab: string): DraftSummary[] {
  if (tab === 'all') return drafts
  const status = (d: DraftSummary) => (d.status ?? '').toLowerCase()
  if (tab === 'rejected') return drafts.filter((d) => status(d) === DRAFT_STATUS.REJECTED)
  if (tab === 'accepted') return drafts.filter((d) => status(d) === DRAFT_STATUS.ACCEPTED)
  return drafts.filter(
    (d) =>
      status(d) !== DRAFT_STATUS.REJECTED &&
      status(d) !== DRAFT_STATUS.ACCEPTED &&
      status(d) !== DRAFT_STATUS.SKIPPED
  )
}

function getEmptyStateMessage(filter: string): string {
  switch (filter) {
    case 'all':
      return 'No drafts yet. Trigger Gmail poll and draft generation from the backend.'
    case 'rejected':
      return 'No rejected drafts.'
    case 'accepted':
      return 'No accepted drafts.'
    case 'ignored':
      return 'No ignored emails.'
    case 'pending':
    default:
      return 'No drafts pending review.'
  }
}

export function DraftsPage() {
  const navigate = useNavigate()
  const [filter, setFilter] = useState<string>('pending')
  const { data: drafts = [], isPending, error } = useQuery({
    queryKey: draftsQueryKey,
    queryFn: fetchDrafts,
    enabled: filter !== 'ignored',
  })
  const { data: ignoredEmails = [], isPending: ignoredPending, error: ignoredError } = useQuery({
    queryKey: ignoredEmailsQueryKey,
    queryFn: fetchIgnoredEmails,
    enabled: true,
  })
  const filteredDrafts = useMemo(() => filterDrafts(drafts, filter), [drafts, filter])

  const queryClient = useQueryClient()
  const skipMutation = useMutation({
    mutationFn: skipDraft,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: draftsQueryKey }),
  })

  const counts = useMemo(() => {
    const status = (d: DraftSummary) => (d.status ?? '').toLowerCase()
    const pending = drafts.filter(
      (d) =>
        status(d) !== DRAFT_STATUS.REJECTED &&
        status(d) !== DRAFT_STATUS.ACCEPTED &&
        status(d) !== DRAFT_STATUS.SKIPPED
    ).length
    const accepted = drafts.filter((d) => status(d) === DRAFT_STATUS.ACCEPTED).length
    const rejected = drafts.filter((d) => status(d) === DRAFT_STATUS.REJECTED).length
    return {
      all: drafts.length,
      pending,
      accepted,
      rejected,
      ignored: ignoredEmails.length,
    }
  }, [drafts, ignoredEmails.length])

  const showDrafts = filter !== 'ignored'
  const showIgnored = filter === 'ignored'
  const loading = showDrafts ? isPending : ignoredPending
  const listError = showDrafts ? error : ignoredError
  const emptyDrafts = showDrafts && filteredDrafts.length === 0
  const emptyIgnored = showIgnored && ignoredEmails.length === 0

  return (
    <div className="flex flex-col min-w-0 pl-56">
      <div className="flex flex-wrap items-center gap-2 mb-4">
        {FILTER_TABS.map((tab) => {
          const count = counts[tab as keyof typeof counts] ?? 0
          const isActive = filter === tab
          return (
            <button
              key={tab}
              type="button"
              onClick={() => setFilter(tab)}
              className={`inline-flex items-center gap-1.5 px-3 py-1.5 text-sm font-medium rounded-lg transition-colors shrink-0 ${
                isActive
                  ? 'bg-orange-600 text-white'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              <span>{FILTER_TAB_LABELS[tab]}</span>
              <span
                className={`min-w-[1.25rem] text-center text-xs font-semibold rounded-full px-1.5 py-0.5 ${
                  isActive ? 'bg-orange-400/50 text-white' : 'bg-gray-200 text-gray-600'
                }`}
              >
                {count}
              </span>
            </button>
          )
        })}
      </div>
      {listError && <p className="text-sm text-red-600 mb-2">{listError.message}</p>}
      {loading && (
        <p className="text-sm text-gray-500 mb-2">
          {showDrafts ? 'Loading drafts…' : 'Loading ignored emails…'}
        </p>
      )}
      {!loading && emptyDrafts && !listError && (
        <p className="text-sm text-gray-500">{getEmptyStateMessage(filter)}</p>
      )}
      {!loading && emptyIgnored && !listError && (
        <p className="text-sm text-gray-500">{getEmptyStateMessage(filter)}</p>
      )}
      {showDrafts && !loading && (
        <ul className="space-y-2">
          {filteredDrafts.map((d) => (
            <DraftCard
              key={d.emailId}
              draft={d}
              onClick={() => navigate(`/drafts/${encodeURIComponent(d.emailId)}`)}
              onSkip={() => skipMutation.mutate(d.emailId)}
            />
          ))}
        </ul>
      )}
      {showIgnored && !loading && ignoredEmails.length > 0 && (
        <ul className="space-y-2">
          {ignoredEmails.map((e) => (
            <li
              key={e.emailId}
              className="w-full text-left rounded-lg border border-gray-200 bg-gray-50 p-4"
            >
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-xs text-gray-400">{e.date ? formatDateTime(e.date) : ''}</span>
              </div>
              <p className="font-medium text-gray-800 mt-1 truncate">{e.from}</p>
              <p className="text-sm text-gray-600 truncate">{e.subject}</p>
              {e.snippet && <p className="text-sm text-gray-500 truncate mt-1">{e.snippet}</p>}
              <p className="text-xs text-gray-500 mt-2">Ignored (no draft)</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
