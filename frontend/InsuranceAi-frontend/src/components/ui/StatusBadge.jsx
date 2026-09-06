import styles from './StatusBadge.module.css'

const STATUS_TONE = {
  ACTIVE: 'success',
  CONFIRMED: 'success',
  PAID: 'success',
  APPROVED: 'success',

  PENDING: 'warning',
  UNDER_REVIEW: 'warning',
  SUBMITTED: 'warning',
  DRAFT: 'warning',

  EXPIRED: 'neutral',
  LAPSED: 'neutral',

  CANCELLED: 'danger',
  REJECTED: 'danger',
}

function formatStatus(status) {
  return status
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function StatusBadge({ status }) {
  const tone = STATUS_TONE[status] ?? 'neutral'
  return (
    <span className={`${styles.badge} ${styles[tone]}`}>
      <span className={styles.dot} aria-hidden="true" />
      {formatStatus(status)}
    </span>
  )
}
