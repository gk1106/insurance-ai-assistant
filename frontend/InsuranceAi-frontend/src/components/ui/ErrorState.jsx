import { AlertIcon } from './Icon'
import styles from './ErrorState.module.css'

export function ErrorState({ message, onRetry }) {
  return (
    <div className={styles.container} role="alert">
      <div className={styles.iconWrap}>
        <AlertIcon width={18} height={18} />
      </div>
      <p className={styles.message}>{message}</p>
      {onRetry && (
        <button type="button" className={styles.retryButton} onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  )
}
