import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import { ThemeToggle } from '../components/ui/ThemeToggle'
import styles from './RegisterPage.module.css'

function extractErrorMessage(err) {
  const data = err.response?.data
  if (data?.fieldErrors?.length) {
    return data.fieldErrors.map((fieldError) => fieldError.message).join(' ')
  }
  return data?.message || 'Unable to create account. Please try again.'
}

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)

    try {
      await register(username, email, password)
      navigate('/dashboard', { replace: true })
    } catch (err) {
      setError(extractErrorMessage(err))
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
        <p className={styles.subtitle}>Create your account</p>

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
          <span className={styles.label}>Email</span>
          <input
            className={styles.input}
            type="email"
            name="email"
            autoComplete="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
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
            autoComplete="new-password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
            minLength={8}
            disabled={isSubmitting}
          />
        </label>

        <button className={styles.submit} type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </button>

        <p className={styles.switch}>
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </form>
    </div>
  )
}
