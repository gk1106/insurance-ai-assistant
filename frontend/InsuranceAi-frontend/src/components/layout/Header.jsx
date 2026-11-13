import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Bell, ChevronDown, Search, Menu, LogOut, Settings } from 'lucide-react'
import { useAuth } from '../../hooks/useAuth'
import { ThemeToggle } from '../ui/ThemeToggle'
import styles from './Header.module.css'
const pages = {
  dashboard: 'Dashboard',
  assistant: 'AI Assistant',
  policies: 'My Policies',
  claims: 'Claims',
  calculator: 'Premium Calculator',
  documents: 'Documents',
  history: 'Chat History',
  settings: 'Settings',
  renewals: 'Renewals',
}
export function Header({ onMenuClick }) {
  const { user, logout } = useAuth()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [panel, setPanel] = useState(null)
  function submit(e) {
    e.preventDefault()
    const match = Object.entries(pages).find(([, title]) =>
      title.toLowerCase().includes(search.trim().toLowerCase()),
    )
    if (search.trim() && match) {
      navigate('/' + match[0])
      setSearch('')
    }
  }
  return (
    <header className={styles.header}>
      <button
        type="button"
        className={styles.menuButton}
        onClick={onMenuClick}
        aria-label="Toggle navigation"
        aria-controls="main-navigation"
      >
        <Menu size={20} />
      </button>
      <div className={styles.pageName}>
        <span>Workspace</span>
        <strong>{pages[pathname.split('/')[1]] || 'Overview'}</strong>
      </div>
      <div className={styles.spacer} />
      <form className={styles.search} onSubmit={submit} role="search">
        <Search size={16} />
        <input
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search pages..."
          aria-label="Search pages"
        />
        {search && (
          <div className={styles.results}>
            {Object.entries(pages)
              .filter(([, name]) =>
                name.toLowerCase().includes(search.toLowerCase()),
              )
              .map(([path, name]) => (
                <Link key={path} to={'/' + path} onClick={() => setSearch('')}>
                  {name}
                </Link>
              ))}
            {!Object.values(pages).some((n) =>
              n.toLowerCase().includes(search.toLowerCase()),
            ) && <p>No pages found.</p>}
          </div>
        )}
      </form>
      <ThemeToggle />
      <div className={styles.panelWrap}>
        <button
          className="icon-button"
          aria-label="Notifications"
          aria-expanded={panel === 'notifications'}
          onClick={() =>
            setPanel(panel === 'notifications' ? null : 'notifications')
          }
        >
          <Bell size={18} />
        </button>
        {panel === 'notifications' && (
          <div className={styles.dropdown}>
            <strong>Notifications</strong>
            <p>No new notifications.</p>
            <Link to="/renewals" onClick={() => setPanel(null)}>
              View upcoming renewals
            </Link>
          </div>
        )}
      </div>
      <div className={styles.panelWrap}>
        <button
          className={styles.profileButton}
          onClick={() => setPanel(panel === 'profile' ? null : 'profile')}
          aria-expanded={panel === 'profile'}
          aria-label="Open profile menu"
        >
          <span className={styles.avatar}>
            {user?.username?.slice(0, 2).toUpperCase()}
          </span>
          <span className={styles.accountName}>{user?.username}</span>
          <ChevronDown size={14} />
        </button>
        {panel === 'profile' && (
          <div className={styles.dropdown}>
            <Link to="/settings" onClick={() => setPanel(null)}>
              <Settings size={15} /> Account settings
            </Link>
            <button onClick={logout}>
              <LogOut size={15} /> Log out
            </button>
          </div>
        )}
      </div>
    </header>
  )
}
