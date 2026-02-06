import { useState, useEffect } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'react-toastify'
import { API_BASE } from '@/constants/api'
import { draftsQueryKey, rejectDraft, sendDraft, updateDraft } from '@/api/drafts'
import { htmlToPlainText } from '@/utils/html'
import { displayNameFromFrom } from '@/utils/email'
import type { DraftDetail } from '@/types/drafts'

export function ReviewDraftPage() {
  const { emailId } = useParams<{ emailId: string }>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [detail, setDetail] = useState<DraftDetail | null>(null)
  const rejectMutation = useMutation({
    mutationFn: () => rejectDraft(emailId!),
    onSuccess: () => {
      toast.success('Draft rejected')
      navigate('/drafts')
      setTimeout(() => {
        void queryClient.invalidateQueries({ queryKey: draftsQueryKey })
      }, 1000)
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : 'Failed to reject draft'),
  })
  const [isEditing, setIsEditing] = useState(false)
  const [editDraftText, setEditDraftText] = useState('')
  const [editDraftSubject, setEditDraftSubject] = useState('')
  const saveMutation = useMutation({
    mutationFn: (payload: { draftText: string; draftSubject: string }) =>
      updateDraft(emailId!, payload),
    onSuccess: (_, { draftText, draftSubject }) => {
      setDetail((prev) =>
        prev
          ? {
              ...prev,
              draft: {
                ...prev.draft,
                draftText: draftText ?? prev.draft?.draftText,
                draftSubject: draftSubject ?? prev.draft?.draftSubject,
              },
            }
          : null
      )
      setIsEditing(false)
      toast.success('Draft saved')
      void queryClient.invalidateQueries({ queryKey: draftsQueryKey })
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : 'Failed to save draft'),
  })
  const sendMutation = useMutation({
    mutationFn: () => sendDraft(emailId!),
    onSuccess: () => {
      toast.success('Email sent')
      void queryClient.invalidateQueries({ queryKey: draftsQueryKey })
      navigate('/drafts')
    },
    onError: (e) => toast.error(e instanceof Error ? e.message : 'Failed to send email'),
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!emailId) {
      setError('Missing draft ID')
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    fetch(`${API_BASE}/drafts/${encodeURIComponent(emailId)}`)
      .then((r) => (r.ok ? r.json() : Promise.reject(new Error('Not found'))))
      .then(setDetail)
      .catch((e) => {
        setError(e instanceof Error ? e.message : 'Not found')
        setDetail(null)
      })
      .finally(() => setLoading(false))
  }, [emailId])

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-gray-500">Loading draft…</p>
      </div>
    )
  }

  if (error || !detail) {
    return (
      <div className="space-y-4">
        <Link to="/drafts" className="text-sm text-orange-600 hover:text-orange-700">
          ← Back to Drafts
        </Link>
        <p className="text-red-600">{error ?? 'Draft not found.'}</p>
      </div>
    )
  }

  const senderName = displayNameFromFrom(detail.email?.from)

  return (
    <div className="space-y-6 pl-56">
      <div>
        <Link
          to="/drafts"
          className="inline-flex items-center text-sm text-orange-600 hover:text-orange-700 mb-2"
        >
          ← Back to Drafts
        </Link>
        <h1 className="text-2xl font-semibold text-gray-800">Review Draft</h1>
        <p className="text-sm text-gray-500 mt-1">Analyzing email from {senderName}</p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 min-h-0">
        {/* Original Email */}
        <div className="bg-white rounded-lg border border-gray-200 p-6 flex flex-col min-h-0">
          <div className="flex items-center justify-between gap-2 mb-4 flex-shrink-0">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
              Original Email
            </h2>
            {detail.draft?.category && (
              <span className="text-xs font-medium px-2 py-1 rounded bg-gray-200 text-gray-700">
                {detail.draft.category}
              </span>
            )}
          </div>
          <p className="text-sm text-gray-600 flex-shrink-0">
            <strong>FROM:</strong> {detail.email?.from ?? '—'}
          </p>
          <p className="text-sm text-gray-600 mt-1 flex-shrink-0">
            <strong>SUB:</strong> {detail.email?.subject ?? '—'}
          </p>
          <div className="mt-4 flex-1 min-h-0 overflow-y-auto overflow-x-auto text-sm text-gray-600 whitespace-pre-wrap break-words border-t border-gray-100 pt-4">
            {htmlToPlainText(detail.email?.body) || '—'}
          </div>
        </div>

        {/* AI Generated Draft */}
        <div className="bg-white rounded-lg border border-gray-200 p-6 flex flex-col min-h-0">
          <div className="flex items-center justify-between gap-2 mb-4 flex-shrink-0">
            <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide">
              AI Generated Draft
            </h2>
            {!isEditing ? (
              <button
                type="button"
                onClick={() => {
                  setEditDraftText(detail.draft?.draftText ?? '')
                  setEditDraftSubject(detail.draft?.draftSubject ?? '')
                  setIsEditing(true)
                }}
                className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
              >
                Edit Draft
              </button>
            ) : (
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsEditing(false)}
                  className="px-3 py-1.5 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={() =>
                    saveMutation.mutate({
                      draftText: editDraftText,
                      draftSubject: editDraftSubject,
                    })
                  }
                  disabled={saveMutation.isPending}
                  className="px-3 py-1.5 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:opacity-50"
                >
                  {saveMutation.isPending ? 'Saving…' : 'Save'}
                </button>
              </div>
            )}
          </div>
          {isEditing ? (
            <>
              <label className="text-sm font-medium text-gray-600">
                Subject
              </label>
              <input
                type="text"
                value={editDraftSubject}
                onChange={(e) => setEditDraftSubject(e.target.value)}
                className="mt-1 mb-4 px-3 py-2 text-sm border border-gray-200 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
                placeholder="Draft subject"
              />
              <label className="text-sm font-medium text-gray-600">
                Body
              </label>
              <textarea
                value={editDraftText}
                onChange={(e) => setEditDraftText(e.target.value)}
                rows={14}
                className="mt-1 flex-1 min-h-[200px] px-3 py-2 text-sm border border-gray-200 rounded-lg w-full focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent resize-y"
                placeholder="Draft body"
              />
            </>
          ) : (
            <>
              <p className="text-sm text-gray-600 flex-shrink-0">
                <strong>Subject:</strong> {detail.draft?.draftSubject ?? '—'}
              </p>
              <div className="mt-4 flex-1 min-h-0 overflow-y-auto overflow-x-auto text-sm text-gray-600 whitespace-pre-wrap break-words border-t border-gray-100 pt-4">
                {detail.draft?.draftText || '—'}
              </div>
            </>
          )}
          {!isEditing && (
            <div className="flex flex-wrap items-center gap-3 mt-6 pt-4 border-t border-gray-100">
              <button
                type="button"
                onClick={() => emailId && rejectMutation.mutate()}
                disabled={rejectMutation.isPending}
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 disabled:opacity-50"
              >
                {rejectMutation.isPending ? 'Rejecting…' : 'Reject Draft'}
              </button>
              {/* <button
                type="button"
                className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200"
              >
                Schedule
              </button> */}
              <button
                type="button"
                onClick={() => emailId && sendMutation.mutate()}
                disabled={sendMutation.isPending}
                className="px-4 py-2 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:opacity-50"
              >
                {sendMutation.isPending ? 'Sending…' : 'Approve & Send'}
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
