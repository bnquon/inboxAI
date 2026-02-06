import { useState, useEffect } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { OAUTH_BASE } from '@/constants/api'
import { draftsQueryKey, fetchDrafts } from '@/api/drafts'
import { Layout } from '@/components/Layout'
import { LoginPage } from '@/pages/LoginPage'
import { DraftsPage } from '@/pages/DraftsPage'
import { ReviewDraftPage } from '@/pages/ReviewDraftPage'
import { PreferencesPage } from '@/pages/PreferencesPage'

export default function App() {
  const [authChecked, setAuthChecked] = useState(false)
  const [loggedIn, setLoggedIn] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)

  const { data: drafts } = useQuery({
    queryKey: draftsQueryKey,
    queryFn: fetchDrafts,
    enabled: loggedIn,
  })

  useEffect(() => {
    fetch(`${OAUTH_BASE}/status`)
      .then((r) => {
        setLoggedIn(r.ok)
        setAuthChecked(true)
      })
      .catch(() => {
        setLoggedIn(false)
        setAuthChecked(true)
      })
  }, [])

  const handleSignIn = () => {
    setAuthError(null)
    fetch(`${OAUTH_BASE}/authorize`)
      .then((r) => r.json())
      .then((data: { authUrl?: string; error?: string }) => {
        if (data.authUrl) window.location.href = data.authUrl
        else setAuthError(data.error ?? 'Could not get sign-in URL')
      })
      .catch(() => setAuthError('Could not reach server'))
  }

  if (!authChecked) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <p className="text-gray-500">Checking sign-in…</p>
      </div>
    )
  }

  if (!loggedIn) {
    return <LoginPage onSignIn={handleSignIn} errorMessage={authError} />
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout draftsCount={(drafts?.filter((d) => d.status === 'pending').length ?? 0)} />}>
          <Route index element={<Navigate to="/drafts" replace />} />
          <Route path="drafts" element={<DraftsPage />} />
          <Route path="drafts/:emailId" element={<ReviewDraftPage />} />
          <Route path="preferences" element={<PreferencesPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
