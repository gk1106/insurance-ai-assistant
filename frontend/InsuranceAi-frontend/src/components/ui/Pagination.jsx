import styles from './Pagination.module.css'

export function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null

  return (
    <div className={styles.container}>
      <button
        type="button"
        className={styles.button}
        onClick={() => onPageChange(page - 1)}
        disabled={page <= 0}
      >
        Previous
      </button>
      <span className={styles.label}>
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        className={styles.button}
        onClick={() => onPageChange(page + 1)}
        disabled={page >= totalPages - 1}
      >
        Next
      </button>
    </div>
  )
}
