import { NavLink } from 'react-router-dom'
import { OAUTH_BASE } from '@/constants/api'

interface SidebarProps {
  draftsCount: number
}

async function handleLogout(): Promise<void> {
  await fetch(`${OAUTH_BASE}/logout`, { method: 'POST' })
  window.location.href = '/'
}

export function Sidebar({ draftsCount }: SidebarProps) {
  return (
    <aside className="w-56 fixed flex flex-col h-screen bg-white border-r border-gray-200 shrink-0 pt-4">
      <div className="p-4">
        <span className="font-semibold text-gray-800">InboxAI</span>
      </div>
      <nav className="flex-1 px-3 py-2">
        <NavLink
          to="/drafts"
          className={({ isActive }) =>
            `w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left text-sm font-medium transition-colors ${
              isActive ? 'bg-orange-100 text-orange-700' : 'text-gray-600 hover:bg-gray-100'
            }`
          }
        >
          Drafts
          {draftsCount > 0 && (
            <span className="ml-auto bg-orange-500 text-white text-xs font-medium rounded-full w-6 h-6 flex items-center justify-center">
              {draftsCount}
            </span>
          )}
        </NavLink>
        <NavLink
          to="/preferences"
          className={({ isActive }) =>
            `w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left text-sm font-medium transition-colors ${
              isActive ? 'bg-orange-100 text-orange-700' : 'text-gray-600 hover:bg-gray-100'
            }`
          }
        >
          Preferences
        </NavLink>
      </nav>
      <div className="p-3 border-t border-gray-100">
        <button
          type="button"
          onClick={() => void handleLogout()}
          className="cursor-pointer w-full flex items-center gap-3 px-3 py-2 rounded-lg text-left text-sm text-gray-600 hover:bg-gray-100"
        >
          Log out
        </button>
      </div>
    </aside>
  )
}
