import { Suspense, useEffect, useState } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar } from '../components/layout/Sidebar'
import { Header } from '../components/layout/Header'
import { Spinner } from '../components/ui/Spinner'
import styles from './AppLayout.module.css'

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const location = useLocation()
  useEffect(() => {
    window.scrollTo(0, 0)
  }, [location.pathname])
  useEffect(() => {
    if (!sidebarOpen) return
    const previous = document.activeElement
    const navigation = document.getElementById('main-navigation')
    const nodes = () =>
      [...navigation.querySelectorAll('a, button')].filter(
        (el) => el.getClientRects().length,
      )
    nodes()[0]?.focus()
    function key(event) {
      if (event.key === 'Escape') setSidebarOpen(false)
      if (event.key === 'Tab') {
        const list = nodes(),
          first = list[0],
          last = list.at(-1)
        if (event.shiftKey && document.activeElement === first) {
          event.preventDefault()
          last?.focus()
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault()
          first?.focus()
        }
      }
    }
    function resize() {
      if (window.innerWidth > 768) setSidebarOpen(false)
    }
    document.addEventListener('keydown', key)
    window.addEventListener('resize', resize)
    return () => {
      document.removeEventListener('keydown', key)
      window.removeEventListener('resize', resize)
      previous?.focus()
    }
  }, [sidebarOpen])
  return (
    <div className={styles.shell}>
      <Sidebar open={sidebarOpen} onNavigate={() => setSidebarOpen(false)} />

      {sidebarOpen && (
        <div
          className={styles.backdrop}
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      <div className={styles.main} inert={sidebarOpen}>
        <Header onMenuClick={() => setSidebarOpen((open) => !open)} />
        <main className={styles.content}>
          <Suspense fallback={<Spinner label="Loading page?" />}>
            <Outlet />
          </Suspense>
        </main>
      </div>
    </div>
  )
}
