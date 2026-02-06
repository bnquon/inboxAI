import { useState, useEffect } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'react-toastify'
import {
  preferencesIgnoresQueryKey,
  preferencesSignoffQueryKey,
  fetchIgnorePhrases,
  updateIgnorePhrases,
  fetchSignoff,
  updateSignoff,
} from '@/api/preferences'

export function PreferencesPage() {
  const queryClient = useQueryClient()
  const [newPhrase, setNewPhrase] = useState('')
  const [saving, setSaving] = useState(false)
  const [signoffDraft, setSignoffDraft] = useState('')
  const [savingSignoff, setSavingSignoff] = useState(false)

  const { data: phrases = [], isPending: loading, error } = useQuery({
    queryKey: preferencesIgnoresQueryKey,
    queryFn: fetchIgnorePhrases,
  })

  const { data: signoff = '', isPending: loadingSignoff } = useQuery({
    queryKey: preferencesSignoffQueryKey,
    queryFn: fetchSignoff,
  })

  useEffect(() => {
    setSignoffDraft(signoff)
  }, [signoff])

  const handleAdd = () => {
    const trimmed = newPhrase.trim()
    if (!trimmed) return
    if (phrases.includes(trimmed)) {
      toast.info('Already in list')
      return
    }
    setNewPhrase('')
    save([...phrases, trimmed])
  }

  const handleRemove = (index: number) => {
    save(phrases.filter((_, i) => i !== index))
  }

  const save = async (list: string[]) => {
    setSaving(true)
    try {
      await updateIgnorePhrases(list)
      await queryClient.invalidateQueries({ queryKey: preferencesIgnoresQueryKey })
      toast.success('Saved')
    } catch {
      toast.error('Failed to save')
    } finally {
      setSaving(false)
    }
  }

  const saveSignoff = async () => {
    setSavingSignoff(true)
    try {
      await updateSignoff(signoffDraft.trim())
      await queryClient.invalidateQueries({ queryKey: preferencesSignoffQueryKey })
      toast.success('Sign-off saved')
    } catch {
      toast.error('Failed to save sign-off')
    } finally {
      setSavingSignoff(false)
    }
  }

  if (loading) {
    return (
      <div className="pl-56 py-8">
        <p className="text-sm text-gray-500">Loading preferences…</p>
      </div>
    )
  }
  if (error) {
    return (
      <div className="pl-56 py-8">
        <p className="text-sm text-red-600">{error instanceof Error ? error.message : 'Failed to load'}</p>
      </div>
    )
  }

  return (
    <div className="pl-56 w-full">
      <h1 className="text-2xl font-semibold text-gray-800">Preferences</h1>
      <p className="text-sm text-gray-500 mt-1 mb-6">
        Email ignore rules and default sign-off for drafts.
      </p>

      <div className="bg-white rounded-lg border border-gray-200 p-4 mb-6">
        <p className="text-sm font-medium text-gray-700 mb-2">Email Ignore Rules</p>
        <p className="text-xs text-gray-500 mb-3">
          Emails that match these descriptions (in plain English) will be ignored and not sent to draft generation.
        </p>
        <div className="flex gap-2 mb-4">
          <input
            type="text"
            value={newPhrase}
            onChange={(e) => setNewPhrase(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleAdd()}
            placeholder="e.g. github emails, newsletters"
            className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
          />
          <button
            type="button"
            onClick={handleAdd}
            className="px-3 py-2 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700"
          >
            Add
          </button>
        </div>
        {saving && <p className="text-xs text-gray-500 mb-2">Saving…</p>}
        <ul className="space-y-2">
          {phrases.length === 0 ? (
            <li className="text-sm text-gray-500">No ignore rules yet. Add one above.</li>
          ) : (
            phrases.map((phrase, index) => (
              <li
                key={`${phrase}-${index}`}
                className="flex items-center justify-between gap-2 py-2 px-3 bg-gray-50 rounded-lg text-sm text-gray-800"
              >
                <span>{phrase}</span>
                <button
                  type="button"
                  onClick={() => handleRemove(index)}
                  className="text-gray-500 hover:text-red-600 text-xs font-medium"
                >
                  Remove
                </button>
              </li>
            ))
          )}
        </ul>
      </div>

      <div className="bg-white rounded-lg border border-gray-200 p-4">
        <p className="text-sm font-medium text-gray-700 mb-2">Email sign-off</p>
        <p className="text-xs text-gray-500 mb-3">
          Default closing line(s) appended to draft emails (e.g. &quot;Sincerely, Your Name&quot;).
        </p>
        <div className="flex gap-2">
          <input
            type="text"
            value={signoffDraft}
            onChange={(e) => setSignoffDraft(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && saveSignoff()}
            placeholder="e.g. Sincerely, Your Name"
            className="flex-1 px-3 py-2 text-sm border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-500 focus:border-transparent"
            disabled={loadingSignoff}
          />
          <button
            type="button"
            onClick={saveSignoff}
            disabled={loadingSignoff || savingSignoff}
            className="px-3 py-2 text-sm font-medium text-white bg-orange-600 rounded-lg hover:bg-orange-700 disabled:opacity-50"
          >
            {savingSignoff ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  )
}
