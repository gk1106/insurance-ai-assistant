import axios from 'axios'
import { clearStoredSession, getStoredSession } from './authStorage'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
})

apiClient.interceptors.request.use((config) => {
  const session = getStoredSession()
  if (session?.token) {
    config.headers.Authorization = `Bearer ${session.token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    // This backend returns 403 (not 401) for missing/invalid/expired auth, since
    // no AuthenticationEntryPoint is configured — the same status a properly
    // authenticated-but-under-privileged user also gets. Only treat a 403 as a
    // dead session (and force re-login) when there's no valid session left
    // client-side; otherwise it's a legitimate role-based access error the
    // calling page should display itself.
    const sessionIsDead = status === 401 || (status === 403 && !getStoredSession())

    if (sessionIsDead) {
      clearStoredSession()
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)
