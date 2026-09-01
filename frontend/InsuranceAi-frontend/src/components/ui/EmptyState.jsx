import styles from './EmptyState.module.css'

export function EmptyState({ title, message }) {
  return (
    <div className={styles.container} role="status">
      <p className={styles.title}>{title}</p>
      {message && <p className={styles.message}>{message}</p>}
    </div>
  )
}
