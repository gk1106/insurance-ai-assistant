import { InboxIcon } from './Icon'
import styles from './EmptyState.module.css'

export function EmptyState({ title, message }) {
  return (
    <div className={styles.container} role="status">
      <div className={styles.iconWrap}>
        <InboxIcon width={20} height={20} />
      </div>
      <p className={styles.title}>{title}</p>
      {message && <p className={styles.message}>{message}</p>}
    </div>
  )
}
