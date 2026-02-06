const OAUTH_ERROR_MESSAGES = {
  missing_code: 'Sign-in was cancelled or no code was received.',
  config_incomplete: 'Server OAuth is not configured.',
  exchange_failed: 'Token exchange failed. Try again.',
  unexpected: 'Something went wrong. Try again.',
} as const

type OAuthErrorCode = keyof typeof OAUTH_ERROR_MESSAGES

function getErrorMessage(errorParam: string | null): string | null {
  if (!errorParam) return null
  const message = OAUTH_ERROR_MESSAGES[errorParam as OAuthErrorCode]
  return message ?? `Error: ${errorParam}`
}

interface LoginPageProps {
  onSignIn: () => void
  errorMessage: string | null
}

export function LoginPage({ onSignIn, errorMessage }: LoginPageProps) {
  const params = new URLSearchParams(window.location.search)
  const errorFromQuery = getErrorMessage(params.get('error'))
  const displayError = errorFromQuery ?? errorMessage

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm rounded-lg border border-gray-200 bg-white p-8 shadow-sm">
        <h1 className="text-xl font-semibold text-gray-800 text-center">InboxAI</h1>
        <p className="text-sm text-gray-500 text-center mt-2">
          Sign in with your Google account to continue.
        </p>
        {displayError && (
          <p className="text-sm text-red-600 mt-4 text-center">{displayError}</p>
        )}
        <button
          type="button"
          onClick={onSignIn}
          className="cursor-pointer w-full mt-6 px-4 py-3 bg-orange-600 text-white text-sm font-medium rounded-lg hover:bg-orange-700"
        >
          Sign in with Google
        </button>
      </div>
    </div>
  )
}
