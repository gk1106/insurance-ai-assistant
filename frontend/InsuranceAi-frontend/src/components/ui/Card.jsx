import styles from './Card.module.css'

export function Card({ title, children }) {
  return (
    <section className={styles.card}>
      {title && <h2 className={styles.title}>{title}</h2>}
      {children}
    </section>
  )
}
