import { useTheme } from '../../hooks/useTheme'
import { MoonIcon, SunIcon } from './Icon'
import styles from './ThemeToggle.module.css'

export function ThemeToggle({ className }) {
  const { theme, toggleTheme } = useTheme()
  const isDark = theme === 'dark'

  return (
    <button
      type="button"
      className={`${styles.toggle} ${className || ''}`}
      onClick={toggleTheme}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
    >
      {isDark ? <SunIcon width={17} height={17} /> : <MoonIcon width={17} height={17} />}
    </button>
  )
}
