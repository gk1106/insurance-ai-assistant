import { useState } from 'react'
import { AuthContext } from './AuthContext'
import { authApi } from '../api/authApi'
import { clearStoredSession, getStoredSession, setStoredSession } from '../api/authStorage'

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getStoredSession())

  async function login(username, password) {
    const data = await authApi.login({ username, password })
    const nextSession = {
      token: data.token,
      username: data.username,
      role: data.role,
      expiresAt: data.expiresAt,
    }
    setStoredSession(nextSession)
    setSession(nextSession)
  }

  async function register(username, email, password) {
    const data = await authApi.register({ username, email, password })
    const nextSession = {
      token: data.token,
      username: data.username,
      role: data.role,
      expiresAt: data.expiresAt,
    }
    setStoredSession(nextSession)
    setSession(nextSession)
  }

  function logout() {
    clearStoredSession()
    setSession(null)
  }

  const value = {
    user: session ? { username: session.username, role: session.role } : null,
    isAuthenticated: Boolean(session),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
