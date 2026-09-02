import { useEffect, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { Spinner } from '../components/ui/Spinner'
import { ErrorState } from '../components/ui/ErrorState'
import { EmptyState } from '../components/ui/EmptyState'
import { useAuth } from '../hooks/useAuth'
import { policyApi } from '../api/policyApi'
import { renewalApi } from '../api/renewalApi'
import tableStyles from '../components/ui/Table.module.css'
import formStyles from '../components/ui/Form.module.css'

const STAFF_ROLES = new Set(['ADMIN', 'AGENT'])

function errorMessage(err) {
  return err.response?.data?.message || 'Something went wrong. Please try again.'
}

export function RenewalsPage() {
  const { user } = useAuth()
  const isStaff = STAFF_ROLES.has(user?.role)

  const [policiesState, setPoliciesState] = useState({ status: 'loading', policies: [], error: null })
  const [selectedPolicyId, setSelectedPolicyId] = useState('')

  const [renewalsState, setRenewalsState] = useState({ status: 'idle', data: null, error: null })
  const [refreshIndex, setRefreshIndex] = useState(0)
  const [actionError, setActionError] = useState(null)
  const [pendingActionId, setPendingActionId] = useState(null)

  const [showRequestForm, setShowRequestForm] = useState(false)
  const [requestForm, setRequestForm] = useState({ newEndDate: '', revisedPremiumAmount: '' })
  const [isRequesting, setIsRequesting] = useState(false)

  // The backend only exposes renewals per-policy (GET /renewals?policyId=), with
  // no "list all renewals" endpoint — so this page is built around picking a
  // policy first, then loading that policy's renewal history.
  useEffect(() => {
    let cancelled = false

    async function loadPolicies() {
      setPoliciesState({ status: 'loading', policies: [], error: null })
      try {
        const params = isStaff
          ? { page: 0, size: 200 }
          : { customerId: user?.customerId, page: 0, size: 200 }

        if (!isStaff && !user?.customerId) {
          setPoliciesState({
            status: 'error',
            policies: [],
            error: 'Your account is not linked to a customer record, so no policies can be shown.',
          })
          return
        }

        const result = await policyApi.list(params)
        if (cancelled) return
        setPoliciesState({ status: 'success', policies: result.content, error: null })
        if (result.content.length > 0) {
          setSelectedPolicyId((current) => current || result.content[0].id)
        }
      } catch (err) {
        if (cancelled) return
        setPoliciesState({ status: 'error', policies: [], error: errorMessage(err) })
      }
    }

    loadPolicies()

    return () => {
      cancelled = true
    }
  }, [isStaff, user?.customerId])

  useEffect(() => {
    if (!selectedPolicyId) return undefined
    let cancelled = false

    async function loadRenewals() {
      setRenewalsState({ status: 'loading', data: null, error: null })
      try {
        const result = await renewalApi.listByPolicy(selectedPolicyId, { page: 0, size: 20 })
        if (cancelled) return
        setRenewalsState({ status: 'success', data: result, error: null })
      } catch (err) {
        if (cancelled) return
        setRenewalsState({ status: 'error', data: null, error: errorMessage(err) })
      }
    }

    loadRenewals()

    return () => {
      cancelled = true
    }
  }, [selectedPolicyId, refreshIndex])

  function refresh() {
    setRefreshIndex((n) => n + 1)
  }

  async function handleConfirm(id) {
    setActionError(null)
    setPendingActionId(id)
    try {
      await renewalApi.confirm(id)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handleReject(id) {
    setActionError(null)
    setPendingActionId(id)
    try {
      await renewalApi.reject(id)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handleRequestSubmit(event) {
    event.preventDefault()
    setActionError(null)
    setIsRequesting(true)
    try {
      await policyApi.renew(selectedPolicyId, {
        newEndDate: requestForm.newEndDate,
        revisedPremiumAmount: Number(requestForm.revisedPremiumAmount),
      })
      setRequestForm({ newEndDate: '', revisedPremiumAmount: '' })
      setShowRequestForm(false)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setIsRequesting(false)
    }
  }

  const selectedPolicy = policiesState.policies.find((p) => p.id === selectedPolicyId)

  return (
    <>
      <PageHeader title="Renewals" description="Track and process policy renewals." />

      {policiesState.status === 'loading' && <Spinner label="Loading policies…" />}

      {policiesState.status === 'error' && <ErrorState message={policiesState.error} />}

      {policiesState.status === 'success' && policiesState.policies.length === 0 && (
        <EmptyState
          title="No policies yet"
          message="Renewals can be viewed once at least one policy exists."
        />
      )}

      {policiesState.status === 'success' && policiesState.policies.length > 0 && (
        <>
          <Card>
            <form className={formStyles.form} onSubmit={(e) => e.preventDefault()}>
              <label className={`${formStyles.field} ${formStyles.wide}`}>
                <span className={formStyles.label}>Policy</span>
                <select
                  className={formStyles.select}
                  value={selectedPolicyId}
                  onChange={(e) => {
                    setSelectedPolicyId(e.target.value)
                    setShowRequestForm(false)
                  }}
                >
                  {policiesState.policies.map((policy) => (
                    <option key={policy.id} value={policy.id}>
                      {policy.policyNumber} — {policy.policyType}
                      {isStaff ? ` (${policy.customerName})` : ''} — ends {policy.endDate}
                    </option>
                  ))}
                </select>
              </label>
              <div className={formStyles.actions}>
                <button
                  type="button"
                  className={formStyles.primaryButton}
                  onClick={() => setShowRequestForm((v) => !v)}
                >
                  {showRequestForm ? 'Cancel' : 'Request Renewal'}
                </button>
              </div>
            </form>
          </Card>

          {actionError && <p className={formStyles.error}>{actionError}</p>}

          {showRequestForm && selectedPolicy && (
            <Card title={`Request Renewal — ${selectedPolicy.policyNumber}`}>
              <form className={formStyles.form} onSubmit={handleRequestSubmit}>
                <label className={formStyles.field}>
                  <span className={formStyles.label}>New End Date</span>
                  <input
                    className={formStyles.input}
                    type="date"
                    value={requestForm.newEndDate}
                    onChange={(e) => setRequestForm({ ...requestForm, newEndDate: e.target.value })}
                    required
                  />
                </label>
                <label className={formStyles.field}>
                  <span className={formStyles.label}>Revised Premium</span>
                  <input
                    className={formStyles.input}
                    type="number"
                    min="0.01"
                    step="0.01"
                    value={requestForm.revisedPremiumAmount}
                    onChange={(e) =>
                      setRequestForm({ ...requestForm, revisedPremiumAmount: e.target.value })
                    }
                    required
                  />
                </label>
                <div className={formStyles.actions}>
                  <button type="submit" className={formStyles.primaryButton} disabled={isRequesting}>
                    {isRequesting ? 'Requesting…' : 'Submit Request'}
                  </button>
                </div>
              </form>
            </Card>
          )}

          {renewalsState.status === 'loading' && <Spinner label="Loading renewals…" />}

          {renewalsState.status === 'error' && (
            <ErrorState message={renewalsState.error} onRetry={refresh} />
          )}

          {renewalsState.status === 'success' && renewalsState.data.content.length === 0 && (
            <EmptyState
              title="No renewal history"
              message="No renewals have been requested for this policy yet."
            />
          )}

          {renewalsState.status === 'success' && renewalsState.data.content.length > 0 && (
            <Card title="Renewal History">
              <div className={tableStyles.tableWrapper}>
                <table className={tableStyles.table}>
                  <thead>
                    <tr>
                      <th>Previous End Date</th>
                      <th>New End Date</th>
                      <th>Revised Premium</th>
                      <th>Status</th>
                      <th>Requested</th>
                      <th>Decided</th>
                      {isStaff && <th>Actions</th>}
                    </tr>
                  </thead>
                  <tbody>
                    {renewalsState.data.content.map((renewal) => (
                      <tr key={renewal.id}>
                        <td>{renewal.previousEndDate}</td>
                        <td>{renewal.newEndDate}</td>
                        <td>${Number(renewal.revisedPremiumAmount).toLocaleString()}</td>
                        <td>
                          <StatusBadge status={renewal.status} />
                        </td>
                        <td>{new Date(renewal.requestedAt).toLocaleDateString()}</td>
                        <td>
                          {renewal.decidedAt ? new Date(renewal.decidedAt).toLocaleDateString() : '—'}
                        </td>
                        {isStaff && (
                          <td>
                            {renewal.status === 'PENDING' && (
                              <div className={formStyles.rowActions}>
                                <button
                                  type="button"
                                  className={formStyles.smallButton}
                                  onClick={() => handleConfirm(renewal.id)}
                                  disabled={pendingActionId === renewal.id}
                                >
                                  Confirm
                                </button>
                                <button
                                  type="button"
                                  className={formStyles.smallButton}
                                  onClick={() => handleReject(renewal.id)}
                                  disabled={pendingActionId === renewal.id}
                                >
                                  Reject
                                </button>
                              </div>
                            )}
                          </td>
                        )}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </Card>
          )}
        </>
      )}
    </>
  )
}
