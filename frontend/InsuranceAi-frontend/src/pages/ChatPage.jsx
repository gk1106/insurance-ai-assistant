import { useEffect, useRef, useState } from 'react'
import { PageHeader } from '../components/layout/PageHeader'
import { Card } from '../components/ui/Card'
import { StatusBadge } from '../components/ui/StatusBadge'
import { useAuth } from '../hooks/useAuth'
import { policyAgentApi } from '../api/policyAgentApi'
import formStyles from '../components/ui/Form.module.css'
import styles from './ChatPage.module.css'

const STAFF_ROLES = new Set(['ADMIN', 'AGENT'])
const MAX_TEXTAREA_HEIGHT = 160
const HISTORY_TURNS_FOR_CONTEXT = 6

const STAFF_SUGGESTIONS = [
  'Show me all active policies',
  'List policies expiring in the next 30 days',
  "What's the status of policy POL-2026-A3F3CF4E?",
  'Create a new AUTO policy for a customer',
]

const CUSTOMER_SUGGESTIONS = [
  'Show me my policies',
  "What's the status of my policy?",
  'Is my policy expiring soon?',
]

function errorMessage(err) {
  return err.response?.data?.message || 'Something went wrong talking to the assistant.'
}

// The backend endpoint is single-turn/stateless by design (see backend/.../PolicyAgentService) --
// it has no memory of earlier messages. To keep follow-up questions coherent without touching the
// backend, recent turns are stitched into the outgoing message as plain-text context.
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

function PolicySummary({ policy }) {
  return (
    <div className={styles.policyCard}>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>Policy Number</span>
        <strong>{policy.policyNumber}</strong>
      </div>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>Type</span>
        <span>{policy.policyType}</span>
      </div>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>Status</span>
        <StatusBadge status={policy.status} />
      </div>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>Coverage</span>
        <span>${Number(policy.coverageAmount).toLocaleString()}</span>
      </div>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>Premium</span>
        <span>${Number(policy.premiumAmount).toLocaleString()}</span>
      </div>
      <div className={styles.policyCardRow}>
        <span className={styles.policyCardLabel}>End Date</span>
        <span>{policy.endDate}</span>
      </div>
    </div>
  )
}

function PolicyListSummary({ policies }) {
  if (policies.length === 0) return null
  return (
    <div className={styles.policyCard}>
      {policies.map((policy) => (
        <div key={policy.id} className={styles.policyCardRow}>
          <span>
            {policy.policyNumber} — {policy.policyType}
          </span>
          <StatusBadge status={policy.status} />
        </div>
      ))}
    </div>
  )
}

export function ChatPage() {
  const { user } = useAuth()
  const isStaff = STAFF_ROLES.has(user?.role)
  const suggestions = isStaff ? STAFF_SUGGESTIONS : CUSTOMER_SUGGESTIONS

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

  async function sendToAgent(text, historySnapshot) {
    setIsSending(true)
    try {
      const prompt = buildPromptWithHistory(historySnapshot, text)
      const data = await policyAgentApi.chat({ message: prompt })
      setMessages((prev) => [
        ...prev,
        { id: makeId(), role: 'assistant', content: data.reply, policy: data.policy, policies: data.policies },
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
    sendToAgent(text, historySnapshot)
  }

  function handleRetry() {
    if (!lastFailedMessage || isSending) return
    const text = lastFailedMessage
    const historySnapshot = messages.slice(0, -1)
    setMessages(historySnapshot)
    sendToAgent(text, historySnapshot)
  }

  function handleNewChat() {
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
        description="Ask about policies in plain language — the agent uses the same policy tools and permissions as the rest of the app."
        actions={
          <button type="button" className={formStyles.secondaryButton} onClick={handleNewChat}>
            New Chat
          </button>
        }
      />

      <Card>
        <div className={styles.chatWindow}>
          <div className={styles.chatHeader}>
            <div className={styles.agentAvatar} aria-hidden="true">
              AI
            </div>
            <div>
              <div className={styles.agentName}>Policy Agent</div>
              <div className={styles.agentSubtitle}>
                {isStaff
                  ? 'Search, view, create, and update policies'
                  : 'Search and view your policies'}
              </div>
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
              {messages.map((message) => (
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

                    {message.policy && <PolicySummary policy={message.policy} />}
                    {message.policies && <PolicyListSummary policies={message.policies} />}

                    {message.isError && (
                      <button type="button" className={styles.retryButton} onClick={handleRetry}>
                        Retry
                      </button>
                    )}
                  </div>
                </div>
              ))}

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
              placeholder="Ask about a policy..."
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
