import { NavLink, Link } from "react-router-dom";
import {
  LayoutDashboard,
  Sparkles,
  ShieldCheck,
  ClipboardList,
  Calculator,
  FolderOpen,
  History,
  Settings,
  RefreshCw,
  X,
} from "lucide-react";
import { useAuth } from "../../hooks/useAuth";
import styles from "./Sidebar.module.css";
const items = [
  ["/dashboard", "Dashboard", LayoutDashboard],
  ["/assistant", "AI Assistant", Sparkles],
  ["/policies", "My Policies", ShieldCheck],
  ["/claims", "Claims", ClipboardList],
  ["/calculator", "Premium Calculator", Calculator],
  ["/documents", "Documents", FolderOpen],
  ["/history", "Chat History", History],
  ["/renewals", "Renewals", RefreshCw],
  ["/settings", "Settings", Settings],
];
export function Sidebar({ open, onNavigate }) {
  const { user } = useAuth();
  return (
    <nav
      id="main-navigation"
      className={`${styles.sidebar} ${open ? styles.open : ""}`}
      aria-label="Main navigation"
    >
      <button
        className={`icon-button ${styles.close}`}
        onClick={onNavigate}
        aria-label="Close navigation"
      >
        <X size={18} />
      </button>
      <Link
        to="/dashboard"
        className={styles.brand}
        style={{ textDecoration: "none" }}
        onClick={onNavigate}
      >
        <span className={styles.brandMark}>
          <ShieldCheck size={23} />
        </span>
        <span className={styles.brandName}>
          Insurance AI<span style={{ color: "var(--color-sage)" }}>.</span>
        </span>
      </Link>
      <ul className={styles.navList}>
        {items.map(([to, label, Icon]) => (
          <li key={to}>
            <NavLink
              to={to}
              onClick={onNavigate}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.active : ""}`
              }
            >
              <Icon size={18} className={styles.navIcon} />
              <span>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
      <Link to="/settings" className={styles.profile} onClick={onNavigate}>
        <span className={styles.profileAvatar}>
          {user?.username?.slice(0, 2).toUpperCase()}
        </span>
        <span>
          <strong>{user?.username}</strong>
          <small>{user?.role?.toLowerCase()} account</small>
        </span>
        <Settings
          size={16}
          style={{ marginLeft: "auto", color: "var(--color-text-muted)" }}
        />
      </Link>
    </nav>
  );
}
