import { formatCurrency } from '../utils/currency'
import { useState } from 'react'
import styles from './WorkspacePages.module.css'

const rates = { Health: 0.018, Auto: 0.025, Home: 0.004, Life: 0.006 }
export function PremiumCalculatorPage() {
  const [type, setType] = useState('Health')
  const [coverage, setCoverage] = useState('100000')
  const [deductible, setDeductible] = useState('500')
  const [duration, setDuration] = useState('12')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  function calculate(event) {
    event.preventDefault()
    const c = Number(coverage),
      d = Number(deductible)
    if (
      !Number.isFinite(c) ||
      c < 1000 ||
      c > 10000000 ||
      !Number.isFinite(d) ||
      d < 0 ||
      d >= c
    ) {
      setError(
        'Enter coverage from INR 1,000 to INR 1,00,00,000 and a deductible below your coverage.',
      )
      setResult(null)
      return
    }
    setError('')
    setResult({
      annual: c * rates[type] * (1 - Math.min(d / c, 0.2)),
      type,
      coverage: c,
      deductible: d,
      duration: Number(duration),
    })
  }
  function change(setter, value) {
    setter(value)
    setResult(null)
  }
  return (
    <section className={styles.page} aria-labelledby="calculator-title">
      <h1 id="calculator-title">Premium calculator</h1>
      <p className={styles.intro}>
        Explore how coverage and deductibles affect a sample premium. All
        amounts are in Indian rupees (INR).
      </p>
      <div className={styles.grid}>
        <form className={`${styles.card} ${styles.stack}`} onSubmit={calculate}>
          <h2>Build an illustration</h2>
          <label className={styles.field}>
            Insurance type
            <select
              value={type}
              onChange={(e) => change(setType, e.target.value)}
            >
              {Object.keys(rates).map((t) => (
                <option key={t}>{t}</option>
              ))}
            </select>
          </label>
          <label className={styles.field}>
            Coverage (INR)
            <input
              type="number"
              min="1000"
              max="10000000"
              step="1"
              required
              value={coverage}
              onChange={(e) => change(setCoverage, e.target.value)}
            />
          </label>
          <label className={styles.field}>
            Policy duration
            <select
              value={duration}
              onChange={(e) => change(setDuration, e.target.value)}
            >
              <option value="12">12 months</option>
              <option value="24">24 months</option>
              <option value="36">36 months</option>
            </select>
          </label>
          <label className={styles.field}>
            Deductible (INR)
            <input
              type="number"
              min="0"
              step="1"
              required
              value={deductible}
              onChange={(e) => change(setDeductible, e.target.value)}
            />
          </label>
          {error && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}
          <button className={styles.primary} type="submit">
            Calculate illustration
          </button>
        </form>
        <div className={styles.card} aria-live="polite">
          <span className={styles.badge}>Illustrative only · Not a quote</span>
          {result ? (
            <>
              <span className={styles.amount}>
                {formatCurrency(result.annual / 12)}
              </span>
              <p className={styles.muted}>
                per month · {formatCurrency(result.annual)} per year
              </p>
              <dl className={styles.facts}>
                <dt>Estimated term total</dt>
                <dd>
                  {formatCurrency((result.annual * result.duration) / 12)}
                </dd>
                <dt>Policy duration</dt>
                <dd>{result.duration} months</dd>
                <dt>Insurance type</dt>
                <dd>{result.type}</dd>
                <dt>Coverage</dt>
                <dd>{formatCurrency(result.coverage)}</dd>
                <dt>Deductible</dt>
                <dd>{formatCurrency(result.deductible)}</dd>
              </dl>
            </>
          ) : (
            <p>
              Enter your preferences to see a sample monthly and annual premium.
            </p>
          )}
          <p className={styles.muted}>
            This educational model uses invented annual rates: Health 1.8%, Auto
            2.5%, Home 0.4%, Life 0.6% of coverage, reduced by the
            deductible-to-coverage ratio (maximum 20%). Annual premium is
            divided by 12 for the monthly illustration.
          </p>
          <p className={styles.muted}>
            No insurer pricing, underwriting, taxes or fees are included. An
            insurer must provide your actual quote.
          </p>
        </div>
      </div>
    </section>
  )
}
