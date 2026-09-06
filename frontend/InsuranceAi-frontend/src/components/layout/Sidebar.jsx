import { NavLink } from 'react-router-dom'
import { AssistantIcon, ClaimIcon, DashboardIcon, PolicyIcon, RenewalIcon } from '../ui/Icon'
import styles from './Sidebar.module.css'

const NAV_ITEMS = [
  { to: '/dashboard', label: 'Dashboard', Icon: DashboardIcon },
  { to: '/policies', label: 'Policies', Icon: PolicyIcon },
  { to: '/claims', label: 'Claims', Icon: ClaimIcon },
  { to: '/renewals', label: 'Renewals', Icon: RenewalIcon },
  { to: '/assistant', label: 'AI Assistant', Icon: AssistantIcon },
]

export function Sidebar({ open, onNavigate }) {
  return (
    <nav
      className={`${styles.sidebar} ${open ? styles.open : ''}`}
      aria-label="Main navigation"
    >
      <div className={styles.brand}>
        <span className={styles.brandMark}>IA</span>
        <span className={styles.brandName}>Insurance AI</span>
      </div>
      <ul className={styles.navList}>
        {NAV_ITEMS.map(({ to, label, Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              onClick={onNavigate}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.active : ''}`
              }
            >
              <Icon className={styles.navIcon} />
              <span>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  )
}
