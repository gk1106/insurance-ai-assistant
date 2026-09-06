import styles from './StatusBreakdownBar.module.css'

// A single-row segmented bar showing how a total splits across statuses.
// Reuses the same success/warning/neutral/danger tones as StatusBadge so a
// status always reads as the same color everywhere it appears in the app.
export function StatusBreakdownBar({ title, segments }) {
  const total = segments.reduce((sum, segment) => sum + segment.count, 0)

  return (
    <div className={styles.wrapper}>
      <div className={styles.header}>
        <span className={styles.title}>{title}</span>
        <span className={styles.total}>{total} total</span>
      </div>

      {total === 0 ? (
        <div className={styles.emptyTrack} />
      ) : (
        <div className={styles.track} role="img" aria-label={`${title}: ${segments.map((s) => `${s.count} ${s.label}`).join(', ')}`}>
          {segments
            .filter((segment) => segment.count > 0)
            .map((segment) => (
              <div
                key={segment.label}
                className={`${styles.segment} ${styles[segment.tone]}`}
                style={{ flexGrow: segment.count }}
              />
            ))}
        </div>
      )}

      <div className={styles.legend}>
        {segments.map((segment) => (
          <div key={segment.label} className={styles.legendItem}>
            <span className={`${styles.legendDot} ${styles[segment.tone]}`} />
            <span className={styles.legendLabel}>{segment.label}</span>
            <span className={styles.legendCount}>{segment.count}</span>
          </div>
        ))}
      </div>
    </div>
  )
}
