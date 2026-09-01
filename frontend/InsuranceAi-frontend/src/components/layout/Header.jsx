import { useAuth } from '../../hooks/useAuth'
import styles from './Header.module.css'

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
        <span className={styles.bar} />
        <span className={styles.bar} />
        <span className={styles.bar} />
      </button>

      <div className={styles.spacer} />

      <div className={styles.account}>
        <span className={styles.accountName}>{user?.username ?? 'Guest'}</span>
        <button type="button" className={styles.logoutButton} onClick={logout}>
          Log out
        </button>
      </div>
    </header>
  )
}
