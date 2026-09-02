import { useState } from 'react'
import { AuthContext } from './AuthContext'
import { authApi } from '../api/authApi'
import { clearStoredSession, getStoredSession, setStoredSession } from '../api/authStorage'
import { decodeJwtPayload } from '../api/jwt'

function buildSession(data) {
  const claims = decodeJwtPayload(data.token)
  return {
    token: data.token,
    username: data.username,
    role: data.role,
    expiresAt: data.expiresAt,
    customerId: claims?.customerId ?? null,
  }
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(() => getStoredSession())

  async function login(username, password) {
    const data = await authApi.login({ username, password })
    const nextSession = buildSession(data)
    setStoredSession(nextSession)
    setSession(nextSession)
  }

  async function register(username, email, password) {
    const data = await authApi.register({ username, email, password })
    const nextSession = buildSession(data)
    setStoredSession(nextSession)
    setSession(nextSession)
  }

  function logout() {
    clearStoredSession()
    setSession(null)
  }

  const value = {
    user: session
      ? { username: session.username, role: session.role, customerId: session.customerId }
      : null,
    isAuthenticated: Boolean(session),
    login,
    register,
    logout,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
