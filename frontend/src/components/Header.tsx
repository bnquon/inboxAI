import { useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { toast } from 'react-toastify'
import { API_BASE } from '@/constants/api'
import { draftsQueryKey } from '@/api/drafts'

interface HeaderProps {
  title: string
  showPollButton?: boolean
}

export function Header({ title, showPollButton = false }: HeaderProps) {
  const queryClient = useQueryClient()
  const [polling, setPolling] = useState(false)
  const [refreshing, setRefreshing] = useState(false)

  const handleTriggerPoll = () => {
    setPolling(true)
    fetch(`${API_BASE}/gmail/poll`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('Poll failed'))))
      .then(() => {
        toast.success('Polling retriggered successfully')
        void queryClient.invalidateQueries({ queryKey: draftsQueryKey })
      })
      .catch(() => toast.error('Poll failed'))
      .finally(() => setPolling(false))
    setTimeout(() => {
      void queryClient.invalidateQueries({ queryKey: draftsQueryKey })
    }, 5000)
  }

  const handleRefresh = () => {
    setRefreshing(true)
    queryClient.invalidateQueries({ queryKey: draftsQueryKey }).finally(() => setRefreshing(false))
  }

  const refreshIcon = (
    <svg width="30px" height="30px" viewBox="0 0 45 45" xmlns="http://www.w3.org/2000/svg"><path d="M25 38c-7.2 0-13-5.8-13-13 0-3.2 1.2-6.2 3.3-8.6l1.5 1.3C15 19.7 14 22.3 14 25c0 6.1 4.9 11 11 11 1.6 0 3.1-.3 4.6-1l.8 1.8c-1.7.8-3.5 1.2-5.4 1.2z"/><path d="M34.7 33.7l-1.5-1.3c1.8-2 2.8-4.6 2.8-7.3 0-6.1-4.9-11-11-11-1.6 0-3.1.3-4.6 1l-.8-1.8c1.7-.8 3.5-1.2 5.4-1.2 7.2 0 13 5.8 13 13 0 3.1-1.2 6.2-3.3 8.6z"/><path d="M18 24h-2v-6h-6v-2h8z"/><path d="M40 34h-8v-8h2v6h6z"/></svg>
  )

  return (
    <header className="shrink-0 flex items-center justify-between pl-62 pt-8 pb-4 pr-8">
      <div>
        <h1 className="text-lg font-semibold text-gray-800">{title}</h1>
        <p className="text-sm text-gray-500">Welcome back! Here&apos;s what&apos;s happening today.</p>
      </div>
      {showPollButton && (
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleRefresh}
            disabled={refreshing}
            className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg disabled:opacity-50"
            title="Refresh drafts"
            aria-label="Refresh drafts"
          >
            {refreshIcon}
          </button>
          <button
            type="button"
            onClick={handleTriggerPoll}
            disabled={polling}
            className="px-4 py-2 bg-orange-600 text-white text-sm font-medium rounded-lg hover:bg-orange-700 disabled:opacity-50"
          >
            {polling ? 'Polling…' : 'Poll inbox'}
          </button>
        </div>
      )}
    </header>
  )
}
