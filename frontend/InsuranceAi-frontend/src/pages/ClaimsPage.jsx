import { Fragment, useEffect, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { Spinner } from '../components/ui/Spinner'
import { ErrorState } from '../components/ui/ErrorState'
import { EmptyState } from '../components/ui/EmptyState'
import { Pagination } from '../components/ui/Pagination'
import { useAuth } from '../hooks/useAuth'
import { claimApi } from '../api/claimApi'
import { policyApi } from '../api/policyApi'
import tableStyles from '../components/ui/Table.module.css'
import formStyles from '../components/ui/Form.module.css'

const PAGE_SIZE = 20
const STAFF_ROLES = new Set(['ADMIN', 'AGENT'])

const EMPTY_CLAIM_FORM = { policyId: '', claimAmount: '', incidentDate: '', description: '' }

function errorMessage(err) {
  return err.response?.data?.message || 'Something went wrong. Please try again.'
}

export function ClaimsPage() {
  const { user } = useAuth()
  const isStaff = STAFF_ROLES.has(user?.role)

  const [page, setPage] = useState(0)
  const [refreshIndex, setRefreshIndex] = useState(0)
  const [state, setState] = useState({ status: 'loading', data: null, error: null })
  const [actionError, setActionError] = useState(null)

  const [myPolicies, setMyPolicies] = useState([])
  const [showFileForm, setShowFileForm] = useState(false)
  const [fileForm, setFileForm] = useState(EMPTY_CLAIM_FORM)
  const [isFiling, setIsFiling] = useState(false)

  const [pendingActionId, setPendingActionId] = useState(null)
  const [reviewMode, setReviewMode] = useState(null) // { claimId, type: 'approve' | 'reject' }
  const [approveAmount, setApproveAmount] = useState('')
  const [rejectReason, setRejectReason] = useState('')

  // Claims for a specific customer aren't a first-class backend query, so for a
  // CUSTOMER-role viewer we compose it from their policies + a per-policy claims
  // lookup (both real endpoints); staff use the single "all claims" endpoint.
  useEffect(() => {
    let cancelled = false

    async function loadClaims() {
      setState({ status: 'loading', data: null, error: null })
      try {
        if (isStaff) {
          const result = await claimApi.list({ page, size: PAGE_SIZE })
          if (cancelled) return
          setState({ status: 'success', data: result, error: null })
          return
        }

        if (!user?.customerId) {
          setState({
            status: 'error',
            data: null,
            error: 'Your account is not linked to a customer record, so no claims can be shown.',
          })
          return
        }

        const policies = await policyApi.list({ customerId: user.customerId, page: 0, size: 200 })
        const claimPages = await Promise.all(
          policies.content.map((policy) => claimApi.listByPolicy(policy.id, { page: 0, size: 100 })),
        )
        if (cancelled) return

        const allClaims = claimPages
          .flatMap((claimPage) => claimPage.content)
          .sort((a, b) => new Date(b.filedAt) - new Date(a.filedAt))

        setState({
          status: 'success',
          data: { content: allClaims, totalPages: 1 },
          error: null,
        })
      } catch (err) {
        if (cancelled) return
        setState({ status: 'error', data: null, error: errorMessage(err) })
      }
    }

    loadClaims()

    return () => {
      cancelled = true
    }
  }, [isStaff, user?.customerId, page, refreshIndex])

  useEffect(() => {
    let cancelled = false
    const params = isStaff ? { page: 0, size: 200 } : { customerId: user?.customerId, page: 0, size: 200 }

    if (!isStaff && !user?.customerId) return undefined

    policyApi
      .list(params)
      .then((result) => {
        if (!cancelled) setMyPolicies(result.content)
      })
      .catch(() => {
        if (!cancelled) setMyPolicies([])
      })

    return () => {
      cancelled = true
    }
  }, [isStaff, user?.customerId])

  function refresh() {
    setRefreshIndex((n) => n + 1)
  }

  async function handleFileSubmit(event) {
    event.preventDefault()
    setActionError(null)
    setIsFiling(true)
    try {
      await claimApi.create({
        policyId: fileForm.policyId,
        claimAmount: Number(fileForm.claimAmount),
        incidentDate: fileForm.incidentDate,
        description: fileForm.description,
      })
      setFileForm(EMPTY_CLAIM_FORM)
      setShowFileForm(false)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setIsFiling(false)
    }
  }

  async function handleReview(claimId) {
    setActionError(null)
    setPendingActionId(claimId)
    try {
      await claimApi.review(claimId)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handlePay(claimId) {
    setActionError(null)
    setPendingActionId(claimId)
    try {
      await claimApi.pay(claimId)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handleApproveSubmit(event, claimId) {
    event.preventDefault()
    setActionError(null)
    setPendingActionId(claimId)
    try {
      await claimApi.approve(claimId, { approvedAmount: Number(approveAmount) })
      setReviewMode(null)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  async function handleRejectSubmit(event, claimId) {
    event.preventDefault()
    setActionError(null)
    setPendingActionId(claimId)
    try {
      await claimApi.reject(claimId, { reason: rejectReason })
      setReviewMode(null)
      refresh()
    } catch (err) {
      setActionError(errorMessage(err))
    } finally {
      setPendingActionId(null)
    }
  }

  function openApprove(claim) {
    setReviewMode({ claimId: claim.id, type: 'approve' })
    setApproveAmount(String(claim.claimAmount))
    setActionError(null)
  }

  function openReject(claim) {
    setReviewMode({ claimId: claim.id, type: 'reject' })
    setRejectReason('')
    setActionError(null)
  }

  const colSpan = 7

  return (
    <>
      <PageHeader
        title="Claims"
        description="Review and manage insurance claims."
        actions={
          <button
            type="button"
            className={formStyles.primaryButton}
            onClick={() => setShowFileForm((v) => !v)}
          >
            {showFileForm ? 'Cancel' : 'File a Claim'}
          </button>
        }
      />

      {actionError && <p className={formStyles.error}>{actionError}</p>}

      {showFileForm && (
        <Card title="File a Claim">
          <form className={formStyles.form} onSubmit={handleFileSubmit}>
            <label className={formStyles.field}>
              <span className={formStyles.label}>Policy</span>
              <select
                className={formStyles.select}
                value={fileForm.policyId}
                onChange={(e) => setFileForm({ ...fileForm, policyId: e.target.value })}
                required
              >
                <option value="" disabled>
                  Select a policy
                </option>
                {myPolicies.map((policy) => (
                  <option key={policy.id} value={policy.id}>
                    {policy.policyNumber} — {policy.policyType}
                    {isStaff ? ` (${policy.customerName})` : ''}
                  </option>
                ))}
              </select>
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Claim Amount</span>
              <input
                className={formStyles.input}
                type="number"
                min="0.01"
                step="0.01"
                value={fileForm.claimAmount}
                onChange={(e) => setFileForm({ ...fileForm, claimAmount: e.target.value })}
                required
              />
            </label>

            <label className={formStyles.field}>
              <span className={formStyles.label}>Incident Date</span>
              <input
                className={formStyles.input}
                type="date"
                value={fileForm.incidentDate}
                onChange={(e) => setFileForm({ ...fileForm, incidentDate: e.target.value })}
                required
              />
            </label>

            <label className={`${formStyles.field} ${formStyles.wide}`}>
              <span className={formStyles.label}>Description</span>
              <textarea
                className={formStyles.textarea}
                value={fileForm.description}
                onChange={(e) => setFileForm({ ...fileForm, description: e.target.value })}
                maxLength={2000}
                required
              />
            </label>

            <div className={formStyles.actions}>
              <button type="submit" className={formStyles.primaryButton} disabled={isFiling}>
                {isFiling ? 'Filing…' : 'Submit Claim'}
              </button>
            </div>
          </form>
        </Card>
      )}

      {state.status === 'loading' && <Spinner label="Loading claims…" />}

      {state.status === 'error' && (
        <ErrorState message={state.error} onRetry={() => setRefreshIndex((n) => n + 1)} />
      )}

      {state.status === 'success' && state.data.content.length === 0 && (
        <EmptyState title="No claims yet" message="No claims have been filed yet." />
      )}

      {state.status === 'success' && state.data.content.length > 0 && (
        <Card>
          <div className={tableStyles.tableWrapper}>
            <table className={tableStyles.table}>
              <thead>
                <tr>
                  <th>Claim Number</th>
                  <th>Policy Number</th>
                  <th>Claim Amount</th>
                  <th>Approved Amount</th>
                  <th>Incident Date</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {state.data.content.map((claim) => (
                  <Fragment key={claim.id}>
                    <tr>
                      <td>{claim.claimNumber}</td>
                      <td>{claim.policyNumber}</td>
                      <td>${Number(claim.claimAmount).toLocaleString()}</td>
                      <td>
                        {claim.approvedAmount != null
                          ? `$${Number(claim.approvedAmount).toLocaleString()}`
                          : '—'}
                      </td>
                      <td>{claim.incidentDate}</td>
                      <td>
                        <StatusBadge status={claim.status} />
                      </td>
                      <td>
                        {isStaff && (
                          <div className={formStyles.rowActions}>
                            {claim.status === 'SUBMITTED' && (
                              <button
                                type="button"
                                className={formStyles.smallButton}
                                onClick={() => handleReview(claim.id)}
                                disabled={pendingActionId === claim.id}
                              >
                                Start Review
                              </button>
                            )}
                            {claim.status === 'UNDER_REVIEW' && (
                              <>
                                <button
                                  type="button"
                                  className={formStyles.smallButton}
                                  onClick={() => openApprove(claim)}
                                >
                                  Approve
                                </button>
                                <button
                                  type="button"
                                  className={formStyles.smallButton}
                                  onClick={() => openReject(claim)}
                                >
                                  Reject
                                </button>
                              </>
                            )}
                            {claim.status === 'APPROVED' && (
                              <button
                                type="button"
                                className={formStyles.smallButton}
                                onClick={() => handlePay(claim.id)}
                                disabled={pendingActionId === claim.id}
                              >
                                Mark Paid
                              </button>
                            )}
                          </div>
                        )}
                      </td>
                    </tr>

                    {reviewMode?.claimId === claim.id && reviewMode.type === 'approve' && (
                      <tr>
                        <td colSpan={colSpan}>
                          <form
                            className={formStyles.form}
                            onSubmit={(e) => handleApproveSubmit(e, claim.id)}
                          >
                            <label className={formStyles.field}>
                              <span className={formStyles.label}>Approved Amount</span>
                              <input
                                className={formStyles.input}
                                type="number"
                                min="0.01"
                                step="0.01"
                                max={claim.claimAmount}
                                value={approveAmount}
                                onChange={(e) => setApproveAmount(e.target.value)}
                                required
                              />
                            </label>
                            <div className={formStyles.actions}>
                              <button
                                type="submit"
                                className={formStyles.primaryButton}
                                disabled={pendingActionId === claim.id}
                              >
                                Confirm Approval
                              </button>
                              <button
                                type="button"
                                className={formStyles.secondaryButton}
                                onClick={() => setReviewMode(null)}
                              >
                                Close
                              </button>
                            </div>
                          </form>
                        </td>
                      </tr>
                    )}

                    {reviewMode?.claimId === claim.id && reviewMode.type === 'reject' && (
                      <tr>
                        <td colSpan={colSpan}>
                          <form
                            className={formStyles.form}
                            onSubmit={(e) => handleRejectSubmit(e, claim.id)}
                          >
                            <label className={`${formStyles.field} ${formStyles.wide}`}>
                              <span className={formStyles.label}>Rejection Reason</span>
                              <input
                                className={formStyles.input}
                                type="text"
                                maxLength={500}
                                value={rejectReason}
                                onChange={(e) => setRejectReason(e.target.value)}
                                required
                              />
                            </label>
                            <div className={formStyles.actions}>
                              <button
                                type="submit"
                                className={formStyles.dangerButton}
                                disabled={pendingActionId === claim.id}
                              >
                                Confirm Rejection
                              </button>
                              <button
                                type="button"
                                className={formStyles.secondaryButton}
                                onClick={() => setReviewMode(null)}
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

          {isStaff && (
            <Pagination page={page} totalPages={state.data.totalPages} onPageChange={setPage} />
          )}
        </Card>
      )}
    </>
  )
}
