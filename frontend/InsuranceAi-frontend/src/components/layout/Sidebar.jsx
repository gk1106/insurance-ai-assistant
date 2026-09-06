import { NavLink } from 'react-router-dom'
import styles from './Sidebar.module.css'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/policies', label: 'Policies' },
  { to: '/claims', label: 'Claims' },
  { to: '/renewals', label: 'Renewals' },
  { to: '/assistant', label: 'AI Assistant' },
]

export function Sidebar({ open, onNavigate }) {
  return (
    <nav
      className={`${styles.sidebar} ${open ? styles.open : ''}`}
      aria-label="Main navigation"
    >
      <div className={styles.brand}>Insurance AI Assistant</div>
      <ul className={styles.navList}>
        {NAV_ITEMS.map((item) => (
          <li key={item.to}>
            <NavLink
              to={item.to}
              onClick={onNavigate}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.active : ''}`
              }
            >
              {item.label}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
