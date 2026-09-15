import { useEffect, useRef, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { useAuth } from '../hooks/useAuth'
import { policyAgentApi } from '../api/policyAgentApi'
import { claimsAgentApi } from '../api/claimsAgentApi'
import formStyles from '../components/ui/Form.module.css'
import styles from './ChatPage.module.css'

const STAFF_ROLES = new Set(['ADMIN', 'AGENT'])
const MAX_TEXTAREA_HEIGHT = 160
const HISTORY_TURNS_FOR_CONTEXT = 6
const MAX_RESULTS_SHOWN = 8

const STAFF_POLICY_SUGGESTIONS = [
  'Show me all active policies',
  'List policies expiring in the next 30 days',
  "What's the status of policy POL-2026-A3F3CF4E?",
  'Create a new AUTO policy for a customer',
]

const CUSTOMER_POLICY_SUGGESTIONS = [
  'Show me my policies',
  "What's the status of my policy?",
  'Is my policy expiring soon?',
]

const STAFF_CLAIMS_SUGGESTIONS = [
  'Show all claims',
  'Find pending claims',
  'Check the status of a claim',
  'File a new claim',
]

const CUSTOMER_CLAIMS_SUGGESTIONS = [
  'Show my claims',
  'Check claim status',
  'Find pending claims',
  'Get claim details',
]

// Each entry wraps one backend agent endpoint. All UI differences between the two agents (which
// API to call, which structured field names to read off the response, suggested prompts, copy)
// are isolated here so the rest of the component stays agent-agnostic.
const AGENT_CONFIG = {
  policy: {
    label: 'Policy Agent',
    initials: 'PA',
    resultKind: 'policy',
    api: policyAgentApi,
    placeholder: 'Ask about a policy...',
    subtitle: (isStaff) => (isStaff ? 'Search, view, create, and update policies' : 'Search and view your policies'),
    suggestions: (isStaff) => (isStaff ? STAFF_POLICY_SUGGESTIONS : CUSTOMER_POLICY_SUGGESTIONS),
  },
  claims: {
    label: 'Claims Agent',
    initials: 'CA',
    resultKind: 'claim',
    api: claimsAgentApi,
    placeholder: 'Ask about a claim...',
    subtitle: (isStaff) => (isStaff ? 'Search, review, and process claims' : 'File and check the status of your claims'),
    suggestions: (isStaff) => (isStaff ? STAFF_CLAIMS_SUGGESTIONS : CUSTOMER_CLAIMS_SUGGESTIONS),
  },
}

function errorMessage(err) {
  return err.response?.data?.message || 'Something went wrong talking to the assistant.'
}

// Both agent endpoints are single-turn/stateless by design -- neither has memory of earlier
// messages. To keep follow-up questions coherent without touching the backend, recent turns are
// stitched into the outgoing message as plain-text context. The full reply text is kept in state
// even though the UI only shows a short caption when a card is rendered (see hasStructuredResult
// below), so the agent still gets its own prior answers as context.
function buildPromptWithHistory(history, newMessage) {
  const recent = history.slice(-HISTORY_TURNS_FOR_CONTEXT).filter((m) => !m.isError)
  if (recent.length === 0) return newMessage

  const context = recent
    .map((m) => `${m.role === 'user' ? 'User' : 'Assistant'}: ${m.content}`)
    .join('\n')
  return `Previous conversation:\n${context}\n\nNew message: ${newMessage}`
}

function makeId() {
  return crypto.randomUUID()
}

function money(amount) {
  return `$${Number(amount).toLocaleString()}`
}

function formatDate(isoInstant) {
  if (!isoInstant) return '—'
  return new Date(isoInstant).toLocaleDateString()
}

// The primary response component for a single policy lookup/create/update result. Only the
// fields useful at a glance are shown -- no raw DTO fields, no tool/agent internals.
function PolicySummary({ policy }) {
  return (
    <div className={styles.resultCard}>
      <div className={styles.resultCardHeader}>
        <div>
          <span className={styles.resultPrimaryLabel}>{policy.policyNumber}</span>
          <span className={styles.resultSecondaryLabel}>{policy.policyType}</span>
        </div>
        <StatusBadge status={policy.status} />
      </div>
      <div className={styles.resultGrid}>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Coverage</span>
          <strong>{money(policy.coverageAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Premium</span>
          <strong>{money(policy.premiumAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Start Date</span>
          <strong>{policy.startDate}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>End Date</span>
          <strong>{policy.endDate}</strong>
        </div>
      </div>
    </div>
  )
}

// The primary response component for a multi-policy search result -- a compact, scannable list
// of cards rather than a wall of text. Capped so a broad search doesn't flood the conversation.
function PolicyListSummary({ policies }) {
  if (policies.length === 0) return null
  const shown = policies.slice(0, MAX_RESULTS_SHOWN)
  const remaining = policies.length - shown.length

  return (
    <div className={styles.resultListWrapper}>
      <div className={styles.resultList}>
        {shown.map((policy) => (
          <div key={policy.id} className={styles.resultListItem}>
            <div>
              <span className={styles.resultPrimaryLabel}>{policy.policyNumber}</span>
              <span className={styles.resultSecondaryLabel}>{policy.policyType}</span>
            </div>
            <div className={styles.resultListItemMeta}>
              <span className={styles.resultLabel}>{policy.endDate}</span>
              <StatusBadge status={policy.status} />
            </div>
          </div>
        ))}
      </div>
      {remaining > 0 && (
        <p className={styles.resultListFooter}>
          +{remaining} more &mdash; ask to narrow your search (by type, status, or customer) to see them.
        </p>
      )}
    </div>
  )
}

// The primary response component for a single claim lookup/create/update result. Fields shown:
// Claim Number, Policy Number, Status, Claim Amount, Approved Amount, Incident Date, Created and
// Updated dates. Note: claims have no "type" field in the backend model (unlike policies) -- there
// is nothing to show there, so Incident Date is shown in its place.
function ClaimSummary({ claim }) {
  return (
    <div className={styles.resultCard}>
      <div className={styles.resultCardHeader}>
        <div>
          <span className={styles.resultPrimaryLabel}>{claim.claimNumber}</span>
          <span className={styles.resultSecondaryLabel}>{claim.policyNumber}</span>
        </div>
        <StatusBadge status={claim.status} />
      </div>
      <div className={styles.resultGrid}>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Claim Amount</span>
          <strong>{money(claim.claimAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Approved Amount</span>
          <strong>{claim.approvedAmount != null ? money(claim.approvedAmount) : '—'}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Incident Date</span>
          <strong>{claim.incidentDate}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Created</span>
          <strong>{formatDate(claim.createdAt)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Updated</span>
          <strong>{formatDate(claim.updatedAt)}</strong>
        </div>
      </div>
    </div>
  )
}

// The primary response component for a multi-claim search result -- same compact card-list shape
// as PolicyListSummary, capped the same way.
function ClaimListSummary({ claims }) {
  if (claims.length === 0) return null
  const shown = claims.slice(0, MAX_RESULTS_SHOWN)
  const remaining = claims.length - shown.length

  return (
    <div className={styles.resultListWrapper}>
      <div className={styles.resultList}>
        {shown.map((claim) => (
          <div key={claim.id} className={styles.resultListItem}>
            <div>
              <span className={styles.resultPrimaryLabel}>{claim.claimNumber}</span>
              <span className={styles.resultSecondaryLabel}>{claim.policyNumber}</span>
            </div>
            <div className={styles.resultListItemMeta}>
              <span className={styles.resultLabel}>{money(claim.claimAmount)}</span>
              <StatusBadge status={claim.status} />
            </div>
          </div>
        ))}
      </div>
      {remaining > 0 && (
        <p className={styles.resultListFooter}>
          +{remaining} more &mdash; ask to narrow your search (by policy or status) to see them.
        </p>
      )}
    </div>
  )
}

export function ChatPage() {
  const { user } = useAuth()
  const isStaff = STAFF_ROLES.has(user?.role)

  const [selectedAgent, setSelectedAgent] = useState('policy')
  const agent = AGENT_CONFIG[selectedAgent]
  const suggestions = agent.suggestions(isStaff)

  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [isSending, setIsSending] = useState(false)
  const [lastFailedMessage, setLastFailedMessage] = useState(null)

  const messageListRef = useRef(null)
  const textareaRef = useRef(null)

  useEffect(() => {
    const el = messageListRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages, isSending])

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = `${Math.min(el.scrollHeight, MAX_TEXTAREA_HEIGHT)}px`
  }, [input])

  async function sendToAgent(agentKey, text, historySnapshot) {
    setIsSending(true)
    try {
      const prompt = buildPromptWithHistory(historySnapshot, text)
      const data = await AGENT_CONFIG[agentKey].api.chat({ message: prompt })
      const singleResult = data.policy ?? data.claim ?? null
      const listResult = data.policies ?? data.claims ?? null
      setMessages((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: data.reply, singleResult, listResult },
      ])
      setLastFailedMessage(null)
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: errorMessage(err), isError: true },
      ])
      setLastFailedMessage(text)
    } finally {
      setIsSending(false)
    }
  }

  function handleSubmit(rawText) {
    const text = rawText.trim()
    if (!text || isSending) return

    const historySnapshot = messages
    setMessages((prev) => [...prev, { id: makeId(), role: 'user', content: text }])
    setInput('')
    sendToAgent(selectedAgent, text, historySnapshot)
  }

  function handleRetry() {
    if (!lastFailedMessage || isSending) return
    const text = lastFailedMessage
    const historySnapshot = messages.slice(0, -1)
    setMessages(historySnapshot)
    sendToAgent(selectedAgent, text, historySnapshot)
  }

  function handleNewChat() {
    setMessages([])
    setInput('')
    setLastFailedMessage(null)
  }

  // Switching agents starts a fresh conversation -- each backend agent is its own stateless
  // endpoint with its own tools and system prompt, so there's no meaningful way to carry a
  // Policy Agent thread over to the Claims Agent (or vice versa).
  function handleSwitchAgent(agentKey) {
    if (agentKey === selectedAgent || isSending) return
    setSelectedAgent(agentKey)
    setMessages([])
    setInput('')
    setLastFailedMessage(null)
  }

  function handleKeyDown(event) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      handleSubmit(input)
    }
  }

  return (
    <>
      <PageHeader
        title="AI Assistant"
        description="Ask about policies or claims in plain language — each agent uses the same tools and permissions as the rest of the app."
        actions={
          <button type="button" className={formStyles.secondaryButton} onClick={handleNewChat}>
            New Chat
          </button>
        }
      />

      <Card>
        <div className={styles.chatWindow}>
          <div className={styles.chatHeader}>
            <div className={styles.agentIdentity}>
              <div className={styles.agentAvatar} aria-hidden="true">
                {agent.initials}
              </div>
              <div>
                <div className={styles.agentName}>{agent.label}</div>
                <div className={styles.agentSubtitle}>{agent.subtitle(isStaff)}</div>
              </div>
            </div>

            <div className={styles.agentSwitcher} role="tablist" aria-label="Choose an agent">
              {Object.entries(AGENT_CONFIG).map(([key, config]) => (
                <button
                  key={key}
                  type="button"
                  role="tab"
                  aria-selected={selectedAgent === key}
                  className={`${styles.agentTab} ${selectedAgent === key ? styles.agentTabActive : ''}`}
                  onClick={() => handleSwitchAgent(key)}
                  disabled={isSending}
                >
                  {config.label}
                </button>
              ))}
            </div>
          </div>

          {messages.length === 0 ? (
            <div className={styles.emptyState}>
              <p className={styles.emptyTitle}>What would you like to know?</p>
              <p className={styles.emptyMessage}>Try one of these, or type your own question below.</p>
              <div className={styles.suggestions}>
                {suggestions.map((suggestion) => (
                  <button
                    key={suggestion}
                    type="button"
                    className={styles.suggestionChip}
                    onClick={() => handleSubmit(suggestion)}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <div className={styles.messageList} ref={messageListRef} role="log" aria-live="polite">
              {messages.map((message) => {
                const hasSingleResult = Boolean(message.singleResult)
                const hasListResult = Boolean(message.listResult?.length)
                const hasStructuredResult = hasSingleResult || hasListResult
                const showBubble = message.role === 'user' || !hasStructuredResult

                return (
                  <div
                    key={message.id}
                    className={`${styles.messageRow} ${message.role === 'user' ? styles.rowUser : styles.rowAssistant}`}
                  >
                    {message.role === 'assistant' && (
                      <div className={styles.messageAvatar} aria-hidden="true">
                        AI
                      </div>
                    )}
                    <div className={styles.messageColumn}>
                      {/* When a structured result is available, the card below is the primary
                          response -- the model's full paragraph is kept in state (for follow-up
                          context) but not shown, so the conversation stays scannable. */}
                      {showBubble && (
                        <div
                          className={`${styles.bubble} ${
                            message.role === 'user'
                              ? styles.bubbleUser
                              : message.isError
                                ? styles.bubbleError
                                : styles.bubbleAssistant
                          }`}
                        >
                          {message.content}
                        </div>
                      )}

                      {hasSingleResult && (
                        <>
                          <p className={styles.resultCaption}>
                            Here's the {agent.resultKind} you requested.
                          </p>
                          {agent.resultKind === 'policy' ? (
                            <PolicySummary policy={message.singleResult} />
                          ) : (
                            <ClaimSummary claim={message.singleResult} />
                          )}
                        </>
                      )}
                      {hasListResult && (
                        <>
                          <p className={styles.resultCaption}>
                            Found {message.listResult.length} matching{' '}
                            {message.listResult.length === 1 ? agent.resultKind : `${agent.resultKind}s`}.
                          </p>
                          {agent.resultKind === 'policy' ? (
                            <PolicyListSummary policies={message.listResult} />
                          ) : (
                            <ClaimListSummary claims={message.listResult} />
                          )}
                        </>
                      )}

                      {message.isError && (
                        <button type="button" className={styles.retryButton} onClick={handleRetry}>
                          Retry
                        </button>
                      )}
                    </div>
                  </div>
                )
              })}

              {isSending && (
                <div className={`${styles.messageRow} ${styles.rowAssistant}`}>
                  <div className={styles.messageAvatar} aria-hidden="true">
                    AI
                  </div>
                  <div className={`${styles.bubble} ${styles.bubbleAssistant} ${styles.typingBubble}`}>
                    <span className={styles.typingDot} />
                    <span className={styles.typingDot} />
                    <span className={styles.typingDot} />
                  </div>
                </div>
              )}
            </div>
          )}

          <div className={styles.composer}>
            <textarea
              ref={textareaRef}
              className={styles.textarea}
              placeholder={agent.placeholder}
              rows={1}
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={handleKeyDown}
              disabled={isSending}
            />
            <button
              type="button"
              className={styles.sendButton}
              onClick={() => handleSubmit(input)}
              disabled={isSending || !input.trim()}
            >
              Send
            </button>
          </div>
          <p className={styles.hint}>Enter to send &middot; Shift+Enter for a new line</p>
        </div>
      </Card>
    </>
  )
}
