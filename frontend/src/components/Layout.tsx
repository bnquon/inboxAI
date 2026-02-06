import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from './Sidebar'
import { Header } from './Header'

interface LayoutProps {
  draftsCount: number
}

function getPageTitle(pathname: string): string {
  if (pathname.match(/^\/drafts\/[^/]+$/)) return 'Review Draft'
  if (pathname.startsWith('/drafts')) return 'Drafts'
  if (pathname.startsWith('/preferences')) return 'Preferences'
  return 'InboxAI'
}

export function Layout({ draftsCount }: LayoutProps) {
  const { pathname } = useLocation()
  const title = getPageTitle(pathname)

  return (
    <div className="flex min-h-screen bg-gray-50">
      <Sidebar draftsCount={draftsCount} />
      <main className="flex-1 flex flex-col min-w-0">
        <Header title={title} showPollButton={pathname === '/drafts'} />
        <div className="flex-1 overflow-auto p-6">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
