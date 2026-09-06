import { useEffect, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatCard } from '../components/ui/StatCard'
import { StatusBadge } from '../components/ui/StatusBadge'
import { StatusBreakdownBar } from '../components/ui/StatusBreakdownBar'
import { Spinner } from '../components/ui/Spinner'
import { ErrorState } from '../components/ui/ErrorState'
import { EmptyState } from '../components/ui/EmptyState'
import { ClaimIcon, PolicyIcon, UsersIcon } from '../components/ui/Icon'
import { customerApi } from '../api/customerApi'
import { policyApi } from '../api/policyApi'
import { claimApi } from '../api/claimApi'
import tableStyles from '../components/ui/Table.module.css'
import styles from './DashboardPage.module.css'

const RENEWAL_WINDOW_DAYS = 30

// The backend has no count-by-status endpoint, so status breakdowns (active
// policies, pending claims) are computed client-side from the fetched page.
// This size is large enough to cover the full dataset for a learning-project
// scale; `totalElements` (used for the Total* stats) is accurate regardless.
const STATS_PAGE_SIZE = 200

const PENDING_CLAIM_STATUSES = new Set(['SUBMITTED', 'UNDER_REVIEW'])

export function DashboardPage() {
  const [state, setState] = useState({ status: 'loading', data: null, error: null })
  const [refreshIndex, setRefreshIndex] = useState(0)

  useEffect(() => {
    let cancelled = false

    async function loadDashboard() {
      setState({ status: 'loading', data: null, error: null })

      try {
        const [customers, policies, claims, expiringPolicies] = await Promise.all([
          customerApi.list({ page: 0, size: 1 }),
          policyApi.list({ page: 0, size: STATS_PAGE_SIZE }),
          claimApi.list({ page: 0, size: STATS_PAGE_SIZE }),
          policyApi.getExpiring(RENEWAL_WINDOW_DAYS),
        ])

        if (cancelled) return

        const countPolicies = (status) => policies.content.filter((p) => p.status === status).length
        const countClaims = (status) => claims.content.filter((c) => c.status === status).length

        setState({
          status: 'success',
          error: null,
          data: {
            totalCustomers: customers.totalElements,
            totalPolicies: policies.totalElements,
            activePolicies: countPolicies('ACTIVE'),
            totalClaims: claims.totalElements,
            pendingClaims: claims.content.filter((c) => PENDING_CLAIM_STATUSES.has(c.status))
              .length,
            expiringPolicies,
            policyStatusBreakdown: [
              { label: 'Active', count: countPolicies('ACTIVE'), tone: 'success' },
              { label: 'Draft', count: countPolicies('DRAFT'), tone: 'warning' },
              { label: 'Expired', count: countPolicies('EXPIRED'), tone: 'neutral' },
              { label: 'Lapsed', count: countPolicies('LAPSED'), tone: 'neutral' },
              { label: 'Cancelled', count: countPolicies('CANCELLED'), tone: 'danger' },
            ],
            claimStatusBreakdown: [
              { label: 'Submitted', count: countClaims('SUBMITTED'), tone: 'warning' },
              { label: 'Under Review', count: countClaims('UNDER_REVIEW'), tone: 'warning' },
              { label: 'Approved', count: countClaims('APPROVED'), tone: 'success' },
              { label: 'Paid', count: countClaims('PAID'), tone: 'success' },
              { label: 'Rejected', count: countClaims('REJECTED'), tone: 'danger' },
            ],
          },
        })
      } catch (err) {
        if (cancelled) return

        const message =
          err.response?.status === 403
            ? "You don't have permission to view this dashboard. It's available to staff accounts (admin/agent) only."
            : err.response?.data?.message || 'Unable to load dashboard data.'

        setState({ status: 'error', data: null, error: message })
      }
    }

    loadDashboard()

    return () => {
      cancelled = true
    }
  }, [refreshIndex])

  return (
    <>
      <PageHeader
        title="Dashboard"
        description="Overview of policies, claims, and renewals."
      />

      {state.status === 'loading' && <Spinner label="Loading dashboard…" />}

      {state.status === 'error' && (
        <ErrorState message={state.error} onRetry={() => setRefreshIndex((n) => n + 1)} />
      )}

      {state.status === 'success' && (
        <>
          <div className={styles.statGrid}>
            <StatCard label="Total Customers" value={state.data.totalCustomers} icon={UsersIcon} tone="primary" />
            <StatCard label="Total Policies" value={state.data.totalPolicies} icon={PolicyIcon} tone="primary" />
            <StatCard label="Active Policies" value={state.data.activePolicies} icon={PolicyIcon} tone="success" />
            <StatCard label="Total Claims" value={state.data.totalClaims} icon={ClaimIcon} tone="primary" />
            <StatCard label="Pending Claims" value={state.data.pendingClaims} icon={ClaimIcon} tone="warning" />
          </div>

          <div className={styles.breakdownGrid}>
            <Card>
              <StatusBreakdownBar title="Policies by Status" segments={state.data.policyStatusBreakdown} />
            </Card>
            <Card>
              <StatusBreakdownBar title="Claims by Status" segments={state.data.claimStatusBreakdown} />
            </Card>
          </div>

          <Card title={`Policies Approaching Renewal (next ${RENEWAL_WINDOW_DAYS} days)`}>
            {state.data.expiringPolicies.length === 0 ? (
              <EmptyState
                title="No policies expiring soon"
                message={`No active policies end within the next ${RENEWAL_WINDOW_DAYS} days.`}
              />
            ) : (
              <div className={tableStyles.tableWrapper}>
                <table className={tableStyles.table}>
                  <thead>
                    <tr>
                      <th>Policy Number</th>
                      <th>Customer</th>
                      <th>Type</th>
                      <th>End Date</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.data.expiringPolicies.map((policy) => (
                      <tr key={policy.id}>
                        <td>{policy.policyNumber}</td>
                        <td>{policy.customerName}</td>
                        <td>{policy.policyType}</td>
                        <td>{policy.endDate}</td>
                        <td>
                          <StatusBadge status={policy.status} />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}
    </>
  )
}
