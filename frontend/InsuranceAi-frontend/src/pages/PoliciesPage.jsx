import { formatCurrency } from '../utils/currency'
import { Link } from 'react-router-dom'
import { Fragment, useEffect, useRef, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { Spinner } from '../components/ui/Spinner'
import { ErrorState } from '../components/ui/ErrorState'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { useAuth } from '../hooks/useAuth'
import { policyApi } from '../api/policyApi'
import { customerApi } from '../api/customerApi'
import tableStyles from '../components/ui/Table.module.css'
import formStyles from '../components/ui/Form.module.css'

import { PolicyIcon } from '../components/ui/Icon'
import styles from './PoliciesPage.module.css'

const PAGE_SIZE = 20
const STAFF_ROLES = new Set(['ADMIN', 'AGENT'])
const CANCELLABLE_STATUSES = new Set(['ACTIVE', 'DRAFT'])
const RENEWABLE_STATUSES = new Set(['ACTIVE', 'EXPIRED'])

const POLICY_TYPES = ['AUTO', 'HEALTH', 'HOME', 'LIFE', 'TRAVEL']

const EMPTY_CREATE_FORM = {
  customerId: '',
  policyType: 'AUTO',
  coverageAmount: '',
  premiumAmount: '',
  startDate: '',
  endDate: '',
}

function errorMessage(err) {
  return (
    err.response?.data?.message || 'Something went wrong. Please try again.'
  )
}

export function PoliciesPage() {
  const { user } = useAuth()
  const isStaff = STAFF_ROLES.has(user?.role)

  const dialogRef = useRef(null)
  const [selectedId, setSelectedId] = useState(null)
  useEffect(() => {
    if (selectedId && !dialogRef.current.open) dialogRef.current.showModal()
    if (!selectedId && dialogRef.current.open) dialogRef.current.close()
  }, [selectedId])

  const [view, setView] = useState('cards')
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(0)
  const [refreshIndex, setRefreshIndex] = useState(0)
  const [state, setState] = useState({
    status: 'loading',
    data: null,
    error: null,
  })
  const [actionError, setActionError] = useState(null)

  const [customers, setCustomers] = useState([])
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [createForm, setCreateForm] = useState(EMPTY_CREATE_FORM)
  const [isCreating, setIsCreating] = useState(false)

  const [renewingId, setRenewingId] = useState(null)
  const [renewForm, setRenewForm] = useState({
    newEndDate: '',
    revisedPremiumAmount: '',
  })
  const [isRenewing, setIsRenewing] = useState(false)

  const [cancelingId, setCancelingId] = useState(null)

  useEffect(() => {
    let cancelled = false

    async function loadPolicies() {
      setState({ status: 'loading', data: null, error: null })

      if (!isStaff && !user?.customerId) {
        setState({
          status: 'error',
          data: null,
          error:
            'Your account is not linked to a customer record, so no policies can be shown.',
        })
        return
      }

      try {
        const params = isStaff
          ? { page, size: PAGE_SIZE }
          : { customerId: user?.customerId, page, size: PAGE_SIZE }
        const result = await policyApi.list(params)
        if (cancelled) return
        setState({ status: 'success', data: result, error: null })
      } catch (err) {
        if (cancelled) return
        setState({ status: 'error', data: null, error: errorMessage(err) })
      }
    }

    loadPolicies()

    return () => {
      cancelled = true
    }
  }, [isStaff, user?.customerId, page, refreshIndex])

  useEffect(() => {
    if (!isStaff) return
    let cancelled = false

    customerApi
      .list({ page: 0, size: 200 })
      .then((result) => {
        if (!cancelled) setCustomers(result.content)
      })
      .catch(() => {
        if (!cancelled) setCustomers([])
      })

    return () => {
      cancelled = true
    }
  }, [isStaff])

  function refresh() {
    setRefreshIndex((n) => n + 1)
  }

  async function handleCreateSubmit(event) {
    event.preventDefault()
    setActionError(null)
    setIsCreating(true)
    try {
      await policyApi.create({
        customerId: createForm.customerId,
        policyType: createForm.policyType,
        coverageAmount: Number(createForm.coverageAmount),
        premiumAmount: Number(createForm.premiumAmount),
        startDate: createForm.startDate,
        endDate: createForm.endDate,
      })
      setCreateForm(EMPTY_CREATE_FORM)
      setShowCreateForm(false)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setIsCreating(false)
    }
  }

  async function handleCancelPolicy(id) {
    if (!window.confirm('Cancel this policy? This cannot be undone.')) return
    setActionError(null)
    setCancelingId(id)
    try {
      await policyApi.cancel(id)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setCancelingId(null)
    }
  }

  function openRenewForm(policy) {
    setView('table')
    setRenewingId(policy.id)
    setRenewForm({
      newEndDate: '',
      revisedPremiumAmount: String(policy.premiumAmount),
    })
    setActionError(null)
  }

  async function handleRenewSubmit(event, policyId) {
    event.preventDefault()
    setActionError(null)
    setIsRenewing(true)
    try {
      await policyApi.renew(policyId, {
        newEndDate: renewForm.newEndDate,
        revisedPremiumAmount: Number(renewForm.revisedPremiumAmount),
      })
      setRenewingId(null)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setIsRenewing(false)
    }
  }

  const policies = state.data?.content ?? []
  const visiblePolicies = policies.filter(
    (policy) => statusFilter === 'ALL' || policy.status === statusFilter,
  )
  const selected = policies.find((policy) => policy.id === selectedId)

  return (
    <div className={styles.page}>
      <PageHeader
        title="Policies"
        description="Manage insurance policies."
        actions={
          isStaff && (
            <button
              type="button"
              className={formStyles.primaryButton}
              onClick={() => setShowCreateForm((v) => !v)}
            >
              {showCreateForm ? 'Cancel' : 'New Policy'}
            </button>
          )
        }
      />

      {actionError && <p className={formStyles.error}>{actionError}</p>}

      {isStaff && showCreateForm && (
        <Card title="New Policy">
          <form className={formStyles.form} onSubmit={handleCreateSubmit}>
            <label className={formStyles.field}>
              <span className={formStyles.label}>Customer</span>
              <select
                className={formStyles.select}
                value={createForm.customerId}
                onChange={(e) =>
                  setCreateForm({ ...createForm, customerId: e.target.value })
                }
                required
              >
                <option value="" disabled>
                  Select a customer
                </option>
                {customers.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.firstName} {c.lastName} ({c.email})
                  </option>
                ))}
              </select>
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Type</span>
              <select
                className={formStyles.select}
                value={createForm.policyType}
                onChange={(e) =>
                  setCreateForm({ ...createForm, policyType: e.target.value })
                }
              >
                {POLICY_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {type}
                  </option>
                ))}
              </select>
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Coverage Amount (INR)</span>
              <input
                className={formStyles.input}
                type="number"
                min="0.01"
                step="0.01"
                value={createForm.coverageAmount}
                onChange={(e) =>
                  setCreateForm({
                    ...createForm,
                    coverageAmount: e.target.value,
                  })
                }
                required
              />
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Premium Amount (INR)</span>
              <input
                className={formStyles.input}
                type="number"
                min="0.01"
                step="0.01"
                value={createForm.premiumAmount}
                onChange={(e) =>
                  setCreateForm({
                    ...createForm,
                    premiumAmount: e.target.value,
                  })
                }
                required
              />
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Start Date</span>
              <input
                className={formStyles.input}
                type="date"
                value={createForm.startDate}
                onChange={(e) =>
                  setCreateForm({ ...createForm, startDate: e.target.value })
                }
                required
              />
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>End Date</span>
              <input
                className={formStyles.input}
                type="date"
                value={createForm.endDate}
                onChange={(e) =>
                  setCreateForm({ ...createForm, endDate: e.target.value })
                }
                required
              />
            </label>

            <div className={formStyles.actions}>
              <button
                type="submit"
                className={formStyles.primaryButton}
                disabled={isCreating}
              >
                {isCreating ? 'Creating…' : 'Create Policy'}
              </button>
            </div>
          </form>
        </Card>
      )}

      <div className={styles.toolbar}>
        <label className={styles.filter}>
          Status
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            {['ALL', 'ACTIVE', 'DRAFT', 'EXPIRED', 'CANCELLED', 'LAPSED'].map(
              (status) => (
                <option key={status} value={status}>
                  {status === 'ALL'
                    ? 'All statuses'
                    : status.charAt(0) + status.slice(1).toLowerCase()}
                </option>
              ),
            )}
          </select>
        </label>
        <div className={styles.toggle} role="group" aria-label="Policy view">
          <button
            type="button"
            aria-pressed={view === 'cards'}
            onClick={() => setView('cards')}
          >
            Cards
          </button>
          <button
            type="button"
            aria-pressed={view === 'table'}
            onClick={() => setView('table')}
          >
            Table
          </button>
        </div>
      </div>
      {state.status === 'success' && (
        <p className={styles.scope}>
          Showing {visiblePolicies.length} of {policies.length} policies on this
          page. Filters apply to this page.
        </p>
      )}
      {state.status === 'success' &&
        policies.length > 0 &&
        visiblePolicies.length === 0 && (
          <EmptyState
            title="No matching policies"
            message="Choose another status to see policies on this page."
          />
        )}
      {state.status === 'success' &&
        view === 'cards' &&
        visiblePolicies.length > 0 && (
          <div className={styles.grid}>
            {visiblePolicies.map((policy) => (
              <Card key={policy.id} className={styles.policyCard}>
                <div className={styles.cardTop}>
                  <span className={styles.icon}>
                    <PolicyIcon width={24} height={24} />
                  </span>
                  <StatusBadge status={policy.status} />
                </div>
                <h2>
                  {policy.policyType.charAt(0) +
                    policy.policyType.slice(1).toLowerCase()}{' '}
                  insurance
                </h2>
                <p className={styles.number}>{policy.policyNumber}</p>
                {isStaff && (
                  <p className={styles.customer}>{policy.customerName}</p>
                )}
                <dl className={styles.facts}>
                  <div>
                    <dt>Coverage amount</dt>
                    <dd className={styles.coverage}>
                      {formatCurrency(policy.coverageAmount)}
                    </dd>
                  </div>
                  <div>
                    <dt>Premium</dt>
                    <dd>{formatCurrency(policy.premiumAmount)}</dd>
                  </div>
                  <div>
                    <dt>Start date</dt>
                    <dd>{policy.startDate}</dd>
                  </div>
                  <div>
                    <dt>End date</dt>
                    <dd>{policy.endDate}</dd>
                  </div>
                </dl>
                <div className={styles.cardActions}>
                  <button
                    type="button"
                    onClick={() => setSelectedId(policy.id)}
                  >
                    View details
                    <span className={styles.srOnly}>
                      {' '}
                      for {policy.policyNumber}
                    </span>
                  </button>
                  {RENEWABLE_STATUSES.has(policy.status) && (
                    <button type="button" onClick={() => openRenewForm(policy)}>
                      Renew
                    </button>
                  )}
                  {isStaff && CANCELLABLE_STATUSES.has(policy.status) && (
                    <button
                      type="button"
                      disabled={cancelingId === policy.id}
                      onClick={() => handleCancelPolicy(policy.id)}
                    >
                      {cancelingId === policy.id ? 'Cancelling?' : 'Cancel'}
                    </button>
                  )}
                </div>
              </Card>
            ))}
          </div>
        )}

      {state.status === 'loading' && <Spinner label="Loading policies…" />}

      {state.status === 'error' && (
        <ErrorState
          message={state.error}
          onRetry={() => setRefreshIndex((n) => n + 1)}
        />
      )}

      {state.status === 'success' && state.data.content.length === 0 && (
        <EmptyState
          title="No policies yet"
          message="No policies have been created yet."
        />
      )}

      {state.status === 'success' &&
        state.data.content.length > 0 &&
        view === 'table' && (
          <Card>
            <div className={tableStyles.tableWrapper}>
              <table className={tableStyles.table}>
                <thead>
                  <tr>
                    <th>Policy Number</th>
                    {isStaff && <th>Customer</th>}
                    <th>Type</th>
                    <th>Coverage</th>
                    <th>Premium</th>
                    <th>Start Date</th>
                    <th>End Date</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {visiblePolicies.map((policy) => (
                    <Fragment key={policy.id}>
                      <tr>
                        <td>
                          <button
                            type="button"
                            className={styles.recordLink}
                            onClick={() => setSelectedId(policy.id)}
                          >
                            {policy.policyNumber}
                          </button>
                        </td>
                        {isStaff && <td>{policy.customerName}</td>}
                        <td>{policy.policyType}</td>
                        <td>{formatCurrency(policy.coverageAmount)}</td>
                        <td>{formatCurrency(policy.premiumAmount)}</td>
                        <td>{policy.startDate}</td>
                        <td>{policy.endDate}</td>
                        <td>
                          <StatusBadge status={policy.status} />
                        </td>
                        <td>
                          <div className={formStyles.rowActions}>
                            {RENEWABLE_STATUSES.has(policy.status) && (
                              <button
                                type="button"
                                className={formStyles.smallButton}
                                onClick={() => openRenewForm(policy)}
                              >
                                Renew
                              </button>
                            )}
                            {isStaff &&
                              CANCELLABLE_STATUSES.has(policy.status) && (
                                <button
                                  type="button"
                                  className={formStyles.smallButton}
                                  onClick={() => handleCancelPolicy(policy.id)}
                                  disabled={cancelingId === policy.id}
                                >
                                  {cancelingId === policy.id
                                    ? 'Cancelling…'
                                    : 'Cancel'}
                                </button>
                              )}
                          </div>
                        </td>
                      </tr>
                      {renewingId === policy.id && (
                        <tr>
                          <td colSpan={isStaff ? 9 : 8}>
                            <form
                              className={formStyles.form}
                              onSubmit={(e) => handleRenewSubmit(e, policy.id)}
                            >
                              <label className={formStyles.field}>
                                <span className={formStyles.label}>
                                  New End Date
                                </span>
                                <input
                                  className={formStyles.input}
                                  type="date"
                                  value={renewForm.newEndDate}
                                  onChange={(e) =>
                                    setRenewForm({
                                      ...renewForm,
                                      newEndDate: e.target.value,
                                    })
                                  }
                                  required
                                />
                              </label>
                              <label className={formStyles.field}>
                                <span className={formStyles.label}>
                                  Revised Premium (INR)
                                </span>
                                <input
                                  className={formStyles.input}
                                  type="number"
                                  min="0.01"
                                  step="0.01"
                                  value={renewForm.revisedPremiumAmount}
                                  onChange={(e) =>
                                    setRenewForm({
                                      ...renewForm,
                                      revisedPremiumAmount: e.target.value,
                                    })
                                  }
                                  required
                                />
                              </label>
                              <div className={formStyles.actions}>
                                <button
                                  type="submit"
                                  className={formStyles.primaryButton}
                                  disabled={isRenewing}
                                >
                                  {isRenewing
                                    ? 'Requesting…'
                                    : 'Request Renewal'}
                                </button>
                                <button
                                  type="button"
                                  className={formStyles.secondaryButton}
                                  onClick={() => setRenewingId(null)}
                                >
                                  Close
                                </button>
                              </div>
                            </form>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </Card>
        )}
      {state.status === 'success' && policies.length > 0 && (
        <Pagination
          page={page}
          totalPages={state.data.totalPages}
          onPageChange={setPage}
        />
      )}
      <dialog
        ref={dialogRef}
        className={styles.dialog}
        aria-labelledby="policy-detail-title"
        onClose={() => setSelectedId(null)}
      >
        {selected && (
          <>
            <div className={styles.modalHeader}>
              <h2 id="policy-detail-title">Policy details</h2>
              <button type="button" onClick={() => setSelectedId(null)}>
                Close
              </button>
            </div>
            <p className={styles.number}>{selected.policyNumber}</p>
            <StatusBadge status={selected.status} />
            <dl className={styles.facts}>
              <div>
                <dt>Insurance type</dt>
                <dd>{selected.policyType}</dd>
              </div>
              {isStaff && (
                <div>
                  <dt>Customer</dt>
                  <dd>{selected.customerName}</dd>
                </div>
              )}
              <div>
                <dt>Coverage amount</dt>
                <dd>{formatCurrency(selected.coverageAmount)}</dd>
              </div>
              <div>
                <dt>Premium amount</dt>
                <dd>{formatCurrency(selected.premiumAmount)}</dd>
              </div>
              <div>
                <dt>Start date</dt>
                <dd>{selected.startDate}</dd>
              </div>
              <div>
                <dt>End date</dt>
                <dd>{selected.endDate}</dd>
              </div>
            </dl>
            <h3>Understand your coverage</h3>
            <p className={styles.scope}>
              This is your recorded coverage limit. Benefits, exclusions, and
              sublimits are defined in your policy documents.
            </p>
            <div className={styles.cardActions}>
              <Link
                className="btn"
                to={`/assistant?prompt=${encodeURIComponent('Explain coverage for policy ' + selected.policyNumber)}`}
              >
                Ask AI about this policy
              </Link>
              <Link className="btn" to="/documents">
                Open document library
              </Link>
            </div>
          </>
        )}
      </dialog>
    </div>
  )
}
