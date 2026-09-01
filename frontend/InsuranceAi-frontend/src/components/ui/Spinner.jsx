import styles from './Spinner.module.css'

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className={styles.container} role="status" aria-live="polite">
      <span className={styles.spinner} aria-hidden="true" />
      <span>{label}</span>
    </div>
  )
}
