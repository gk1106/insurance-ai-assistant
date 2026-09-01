import styles from './StatCard.module.css'

export function StatCard({ label, value }) {
  return (
    <div className={styles.card}>
      <p className={styles.label}>{label}</p>
      <p className={styles.value}>{value}</p>
    </div>
  )
}
