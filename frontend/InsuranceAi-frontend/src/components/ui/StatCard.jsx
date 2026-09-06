import styles from './StatCard.module.css'

export function StatCard({ label, value, icon: IconComponent, tone = 'primary' }) {
  return (
    <div className={styles.card}>
      {IconComponent && (
        <div className={`${styles.iconBadge} ${styles[tone] || styles.primary}`}>
          <IconComponent width={18} height={18} />
        </div>
      )}
      <p className={styles.label}>{label}</p>
      <p className={styles.value}>{value}</p>
    </div>
  )
}
