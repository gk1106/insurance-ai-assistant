import { useAuth } from '../hooks/useAuth'
import { useTheme } from '../hooks/useTheme'
import styles from './WorkspacePages.module.css'

export function SettingsPage() {
  const { user } = useAuth()
  const { theme, toggleTheme } = useTheme()
  return (
    <section className={styles.page} aria-labelledby="settings-title">
      <h1 id="settings-title">Settings</h1>
      <p className={styles.intro}>
        Manage your appearance and understand where your workspace is stored.
      </p>
      <div className={styles.grid}>
        <div className={`${styles.card} ${styles.stack}`}>
          <h2>Appearance</h2>
          <p className={styles.muted}>
            Your theme preference is saved in this browser and applies
            throughout the app.
          </p>
          <div className={styles.row}>
            <span id="dark-mode-label">Dark mode</span>
            <button
              type="button"
              role="switch"
              aria-checked={theme === 'dark'}
              aria-labelledby="dark-mode-label"
              onClick={toggleTheme}
            >
              {theme === 'dark' ? 'On' : 'Off'}
            </button>
          </div>
        </div>
        <div className={styles.card}>
          <h2>Account</h2>
          <dl className={styles.facts}>
            <dt>Username</dt>
            <dd>{user?.username || 'Not signed in'}</dd>
            <dt>Role</dt>
            <dd>{user?.role || 'Unavailable'}</dd>
          </dl>
          <p className={styles.muted}>
            Account profile and password changes are not available here.
          </p>
        </div>
        <div className={styles.card}>
          <h2>Local workspace</h2>
          <p className={styles.muted}>
            Documents and saved chat history stay in this browser, organized by
            username. They are not synced to your account or other devices.
            Clearing browser site data removes them.
          </p>
          <p className={styles.muted}>
            Local data is not encrypted by this app. Other people using this
            browser profile may be able to access it.
          </p>
        </div>
      </div>
    </section>
  )
}
