import { useAuth } from '../../hooks/useAuth'
import { LogoutIcon, MenuIcon } from '../ui/Icon'
import { ThemeToggle } from '../ui/ThemeToggle'
import styles from './Header.module.css'

function initialsFor(username) {
  if (!username) return '?'
  return username.slice(0, 2).toUpperCase()
}

export function Header({ onMenuClick }) {
  const { user, logout } = useAuth()

  return (
    <header className={styles.header}>
      <button
        type="button"
        className={styles.menuButton}
        onClick={onMenuClick}
        aria-label="Toggle navigation"
      >
        <MenuIcon />
      </button>

      <div className={styles.spacer} />

      <div className={styles.account}>
        <ThemeToggle />
        <div className={styles.avatar} aria-hidden="true">
          {initialsFor(user?.username)}
        </div>
        <div className={styles.accountInfo}>
          <span className={styles.accountName}>{user?.username ?? 'Guest'}</span>
          <span className={styles.accountRole}>{user?.role ?? ''}</span>
        </div>
        <button type="button" className={styles.logoutButton} onClick={logout}>
          <LogoutIcon width={15} height={15} />
          <span>Log out</span>
        </button>
      </div>
    </header>
  )
}
