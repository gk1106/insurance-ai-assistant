import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ThemeToggle } from '../components/ui/ThemeToggle'
import styles from './LoginPage.module.css'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await login(username, password)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      const message = err.response?.data?.message || 'Unable to sign in. Please try again.'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={styles.page}>
      <div className={`${styles.blob} ${styles.blob1}`} aria-hidden="true" />
      <div className={`${styles.blob} ${styles.blob2}`} aria-hidden="true" />
      <div className={`${styles.blob} ${styles.blob3}`} aria-hidden="true" />

      <ThemeToggle className={styles.themeToggle} />

      <form className={styles.card} onSubmit={handleSubmit}>
        <div className={styles.brandMark} aria-hidden="true">IA</div>
        <h1 className={styles.title}>Insurance AI Assistant</h1>
        <p className={styles.subtitle}>Sign in to your account</p>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <label className={styles.field}>
          <span className={styles.label}>Username</span>
          <input
            className={styles.input}
            type="text"
            name="username"
            autoComplete="username"
            value={username}
            onChange={(event) => setUsername(event.target.value)}
            required
            disabled={isSubmitting}
          />
        </label>

        <label className={styles.field}>
          <span className={styles.label}>Password</span>
          <input
            className={styles.input}
            type="password"
            name="password"
            autoComplete="current-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            disabled={isSubmitting}
          />
        </label>

        <button className={styles.submit} type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </button>

        <p className={styles.switch}>
          Don't have an account? <Link to="/register">Create one</Link>
        </p>
      </form>
    </div>
  )
}
