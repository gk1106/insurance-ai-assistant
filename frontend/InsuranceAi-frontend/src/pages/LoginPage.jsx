import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import {
  ShieldCheck,
  Sparkles,
  ArrowRight,
  Eye,
  EyeOff,
  Pause,
  Play,
} from 'lucide-react'
import { ThemeToggle } from '../components/ui/ThemeToggle'
import styles from './LoginPage.module.css'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()

  const [showPassword, setShowPassword] = useState(false)
  const [motionPaused, setMotionPaused] = useState(false)
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
      const message =
        err.response?.data?.message || 'Unable to sign in. Please try again.'
      setError(message)
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <main className={`${styles.page} ${motionPaused ? styles.paused : ''}`}>
      <section className={styles.visual} aria-label="About Insurance AI">
        <Link
          className={styles.brand}
          to="/login"
          aria-label="Insurance AI home"
        >
          <span>
            <ShieldCheck size={25} />
          </span>
          Insurance AI<span className={styles.brandDot}>.</span>
        </Link>
        <div className={styles.scene} aria-hidden="true">
          <img
            className={styles.sceneImage}
            src="/images/insurance-shield-3d.webp"
            alt=""
            width="1536"
            height="1024"
            fetchPriority="high"
          />
          <div className={styles.lightSweep} />
          <span className={`${styles.particle} ${styles.particleOne}`} />
          <span className={`${styles.particle} ${styles.particleTwo}`} />
          <span className={`${styles.particle} ${styles.particleThree}`} />
          <span className={styles.orbit} />
        </div>
        <div className={styles.visualCopy}>
          <span className={styles.aiPill}>
            <Sparkles size={14} />
            Your personal insurance guide
          </span>
          <h2>
            A little clarity.
            <br />A lot more confidence.
          </h2>
          <p>
            Understand your coverage. Navigate your claims.
            <br />
            Move forward with answers that make sense.
          </p>
        </div>
        <div className={styles.visualFooter}>
          <span>Policies. Claims. Peace of mind.</span>
          <button
            type="button"
            className={styles.motionButton}
            aria-label={
              motionPaused
                ? 'Play background motion'
                : 'Pause background motion'
            }
            aria-pressed={motionPaused}
            onClick={() => setMotionPaused((p) => !p)}
          >
            {motionPaused ? <Play size={14} /> : <Pause size={14} />}
          </button>
        </div>
      </section>
      <section className={styles.formPanel} aria-label="Account sign in">
        <div className={styles.panelTop}>
          <span>Insurance AI Assistant</span>
          <ThemeToggle className={styles.themeToggle} />
        </div>

        <form
          className={styles.card}
          onSubmit={handleSubmit}
          aria-busy={isSubmitting}
        >
          <div className={styles.brandMark} aria-hidden="true">
            <ShieldCheck size={25} />
          </div>
          <h1 className={styles.title}>Welcome back.</h1>
          <p className={styles.subtitle}>
            Sign in to your Insurance AI account.
          </p>

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
              autoCapitalize="none"
              spellCheck={false}
              placeholder="Enter your username"
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              required
              disabled={isSubmitting}
            />
          </label>

          <label className={styles.field}>
            <span className={styles.label}>Password</span>
            <span className={styles.passwordField}>
              <input
                className={styles.input}
                type={showPassword ? 'text' : 'password'}
                name="password"
                autoComplete="current-password"
                placeholder="Enter your password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                required
                disabled={isSubmitting}
              />
              <button
                type="button"
                className={styles.passwordToggle}
                onClick={() => setShowPassword((s) => !s)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
                disabled={isSubmitting}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </span>
          </label>

          <button
            className={styles.submit}
            type="submit"
            disabled={isSubmitting}
          >
            {isSubmitting ? (
              'Signing in...'
            ) : (
              <>
                Sign in <ArrowRight size={17} />
              </>
            )}
          </button>

          <p className={styles.switch}>
            Don't have an account? <Link to="/register">Create one</Link>
          </p>
        </form>
        <div className={styles.panelFooter}>
          <ShieldCheck size={14} />
          <span>Your insurance, all in one place.</span>
          <span className={styles.footerBrand}>Insurance AI</span>
        </div>
      </section>
    </main>
  )
}
