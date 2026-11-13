import { formatCurrency } from '../utils/currency'
import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowUpRight,
  ArrowRight,
  ShieldCheck,
  Wallet,
  ClipboardList,
  CalendarClock,
  Sparkles,
  FileText,
  Calculator,
  Upload,
  ChevronRight,
  RefreshCw,
} from 'lucide-react'
import { useAuth } from '../hooks/useAuth'
import { dashboardApi } from '../api/dashboardApi'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { Spinner } from '../components/ui/Spinner'
import { ErrorState } from '../components/ui/ErrorState'
import styles from './DashboardPage.module.css'
const quick = [
  [
    'Understand my policy',
    'Coverage, benefits & exclusions',
    FileText,
    '/assistant?prompt=Explain%20my%20insurance%20policy',
  ],
  [
    'Check claim status',
    'Keep track of your claim',
    ClipboardList,
    '/assistant?prompt=Check%20my%20claim%20status',
  ],
  [
    'Calculate premium',
    'Explore a coverage estimate',
    Calculator,
    '/calculator',
  ],
  ['Upload a document', 'Keep your documents together', Upload, '/documents'],
]
function Metric({ icon: Icon, label, value, description, tone }) {
  return (
    <div className={styles.metric}>
      <div className={styles.metricTop}>
        <span>{label}</span>
        <span className={`${styles.metricIcon} ${styles[tone] || ''}`}>
          <Icon size={18} />
        </span>
      </div>
      <strong>{value}</strong>
      <p>
        <span className={`${styles.dot} ${styles[tone] || ''}`} />
        {description}
      </p>
    </div>
  )
}
export function DashboardPage() {
  const { user } = useAuth()
  const [state, setState] = useState({ status: 'loading' })
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let cancelled = false
    dashboardApi
      .load(user)
      .then((data) => {
        if (!cancelled) setState({ status: 'success', ...data })
      })
      .catch((error) => {
        if (!cancelled)
          setState({
            status: 'error',
            error:
              error.response?.data?.message ||
              error.message ||
              'Unable to load your overview.',
          })
      })
    return () => {
      cancelled = true
    }
  }, [user, refresh])
  const policies = state.policies || [],
    claims = state.claims || []
  const active = policies.filter((p) => p.status === 'ACTIVE')
  const pending = claims.filter((c) =>
    ['SUBMITTED', 'UNDER_REVIEW'].includes(c.status),
  )
  const next = active
    .filter(
      (p) =>
        new Date(p.endDate) >= new Date(new Date().toISOString().slice(0, 10)),
    )
    .sort((a, b) => a.endDate.localeCompare(b.endDate))[0]
  const activity = [
    ...policies
      .filter((p) => p.updatedAt || p.createdAt)
      .map((p) => ({
        id: 'p' + p.id,
        title:
          p.updatedAt && p.updatedAt !== p.createdAt
            ? 'Policy updated'
            : 'Policy created',
        reference: p.policyNumber,
        date: p.updatedAt || p.createdAt,
        icon: ShieldCheck,
        status: p.status,
        to: '/policies',
      })),
    ...claims
      .filter((c) => c.filedAt || c.createdAt)
      .map((c) => ({
        id: 'c' + c.id,
        title: 'Claim submitted',
        reference: c.claimNumber,
        date: c.filedAt || c.createdAt,
        icon: ClipboardList,
        status: c.status,
        to: '/claims',
      })),
  ]
    .sort((a, b) => new Date(b.date) - new Date(a.date))
    .slice(0, 5)
  return (
    <>
      <section className={styles.welcome}>
        <div>
          <p className={styles.eyebrow}>YOUR INSURANCE, AT A GLANCE</p>
          <h1>
            Welcome back{user?.username ? `, ${user.username}` : ''}
            <span className={styles.greetingDot}>.</span>
          </h1>
          <p>Your insurance companion, powered by AI.</p>
        </div>
        <Link to="/assistant" className="btn btn-primary">
          <Sparkles size={16} />
          Talk to AI Assistant
          <ArrowUpRight size={16} />
        </Link>
      </section>
      {state.status === 'loading' && (
        <Spinner label="Loading your insurance overview..." />
      )}
      {state.status === 'error' && (
        <ErrorState
          message={state.error}
          onRetry={() => setRefresh((r) => r + 1)}
        />
      )}
      {state.status === 'success' && (
        <div className={styles.statGrid}>
          <Metric
            icon={ShieldCheck}
            label="Active Policies"
            value={active.length}
            description={`${policies.length} policies in your overview`}
            tone="sage"
          />
          <Metric
            icon={Wallet}
            label="Total Coverage"
            value={formatCurrency(
              active.reduce((s, p) => s + Number(p.coverageAmount || 0), 0),
            )}
            description="Across active policies"
          />
          <Metric
            icon={ClipboardList}
            label="Pending Claims"
            value={pending.length}
            description="Submitted or under review"
            tone="amber"
          />
          <Metric
            icon={CalendarClock}
            label="Upcoming Premium"
            value={next ? formatCurrency(next.premiumAmount) : '\u2014'}
            description={
              next
                ? `Current premium - ends ${new Date(next.endDate + 'T00:00:00').toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}`
                : 'No upcoming policy renewals'
            }
            tone="sage"
          />
        </div>
      )}
      <section className={styles.aiCard}>
        <div className={styles.aiContent}>
          <span className={styles.aiLabel}>
            <span />
            YOUR AI INSURANCE COMPANION
          </span>
          <h2>How can I help you today?</h2>
          <p>
            Understand your coverage, navigate a claim, or find answers in your
            policy documents.
          </p>
          <Link to="/assistant" className="btn btn-primary">
            Start a Conversation
            <ArrowRight size={16} />
          </Link>
          <small>
            <ShieldCheck size={13} />
            Connected to your policies and claims
          </small>
        </div>
        <div className={styles.aiVisual} aria-hidden="true">
          <div className={styles.orbit} />
          <div className={styles.orbitTwo} />
          <div className={styles.aiCore}>
            <Sparkles size={36} strokeWidth={1.3} />
          </div>
          <span className={styles.visualTag}>
            <ShieldCheck size={15} />
            Coverage, explained
          </span>
          <span className={styles.visualTagTwo}>
            <FileText size={15} />
            Answers with sources
          </span>
        </div>
      </section>
      <section className={styles.quickSection}>
        <div className={styles.sectionHeading}>
          <h2>How can we help?</h2>
          <span>Quick actions</span>
        </div>
        <div className={styles.quickGrid}>
          {quick.map(([title, description, Icon, to]) => (
            <Link key={title} to={to} className={styles.quickCard}>
              <span className={styles.quickIcon}>
                <Icon size={20} strokeWidth={1.6} />
              </span>
              <strong>{title}</strong>
              <p>{description}</p>
              <ArrowUpRight size={15} className={styles.quickArrow} />
            </Link>
          ))}
        </div>
      </section>
      <div className={styles.lowerGrid}>
        <Card>
          <div className={styles.sectionHeading}>
            <h2>Recent activity</h2>
            <button
              className="icon-button"
              onClick={() => {
                setState({ status: 'loading' })
                setRefresh((r) => r + 1)
              }}
              aria-label="Refresh activity"
            >
              <RefreshCw size={14} />
            </button>
          </div>
          {state.status === 'loading' ? (
            <p className={styles.muted}>Loading activity?</p>
          ) : state.status === 'error' ? (
            <p className={styles.muted}>
              Activity is unavailable. Retry your overview above.
            </p>
          ) : activity.length ? (
            activity.map((item) => (
              <Link to={item.to} key={item.id} className={styles.activity}>
                <span className={styles.activityIcon}>
                  <item.icon size={17} />
                </span>
                <div>
                  <strong>{item.title}</strong>
                  <span className="mono">{item.reference}</span>
                </div>
                <time dateTime={item.date}>
                  {new Date(item.date).toLocaleDateString(undefined, {
                    month: 'short',
                    day: 'numeric',
                  })}
                </time>
                <ChevronRight size={14} />
              </Link>
            ))
          ) : (
            <div className={styles.empty}>
              <FileText size={28} />
              <h3>No recent activity</h3>
              <p>Policy and claim updates will appear here.</p>
            </div>
          )}
        </Card>
        <Card>
          <div className={styles.sectionHeading}>
            <h2>Your coverage</h2>
            <Link to="/policies" aria-label="View all policies">
              <ArrowUpRight size={17} />
            </Link>
          </div>
          <p className={styles.muted}>A clear view of what protects you.</p>
          {state.status === 'success' && active.length ? (
            active.slice(0, 3).map((p) => (
              <Link to="/policies" key={p.id} className={styles.coverage}>
                <span className={styles.coverageIcon}>
                  <ShieldCheck size={19} />
                </span>
                <div>
                  <strong>
                    {p.policyType.charAt(0) +
                      p.policyType.slice(1).toLowerCase()}{' '}
                    insurance
                  </strong>
                  <span className="mono">{p.policyNumber}</span>
                </div>
                <StatusBadge status={p.status} />
              </Link>
            ))
          ) : (
            <div className={styles.empty}>
              <ShieldCheck size={28} />
              <p>
                {state.status === 'error'
                  ? 'Coverage is unavailable.'
                  : state.status === 'loading'
                    ? 'Loading coverage?'
                    : 'Your active policies will appear here.'}
              </p>
            </div>
          )}
          <Link className={styles.textLink} to="/policies">
            Manage policies
            <ArrowRight size={14} />
          </Link>
        </Card>
      </div>
      <footer className={styles.footer}>
        <span>
          <ShieldCheck size={14} />
          Your insurance. All in one place.
        </span>
        <span>Insurance AI</span>
      </footer>
    </>
  )
}
