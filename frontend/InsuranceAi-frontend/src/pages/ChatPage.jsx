import { formatCurrency } from '../utils/currency'
import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import {
  ArrowUp,
  ArrowDown,
  Copy,
  RotateCcw,
  Paperclip,
  Sparkles,
  Plus,
  Trash2,
} from 'lucide-react'
import { StatusBadge } from '../components/ui/StatusBadge'
import { DocumentIcon } from '../components/ui/Icon'
import { useAuth } from '../hooks/useAuth'
import { knowledgeAgentApi } from '../api/knowledgeAgentApi'
import { aiAssistantApi } from '../api/aiAssistantApi'
import { policyAgentApi } from '../api/policyAgentApi'
import { claimsAgentApi } from '../api/claimsAgentApi'
import { renewalAgentApi } from '../api/renewalAgentApi'
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

// Renewals have no "list all" / "list mine" capability on the backend at all (every search
// requires a specific policyId, for every role) -- so these prompts are the same regardless of
// role; the agent will ask which policy if one isn't given, same as it would for a typed question.
const RENEWAL_SUGGESTIONS = [
  'Show my upcoming renewals',
  'Which policies expire this month?',
  'Check renewal status',
  'Get renewal details',
]

const STAFF_ASSISTANT_SUGGESTIONS = [
  'Show me all active policies',
  'Find pending claims',
  'Which policies need renewal?',
  'Check policy POL-2026-A3F3CF4E and its claims',
]

const CUSTOMER_ASSISTANT_SUGGESTIONS = [
  'Show me my policies',
  'Check my claim status',
  'Which of my policies are expiring soon?',
  'My policy is expiring soon and I have a pending claim — what should I do?',
]

// Each entry wraps one backend agent endpoint. All UI differences between the agents (which API
// to call, suggested prompts, copy) are isolated here so the rest of the component stays
// agent-agnostic. None of these carry a fixed "result kind" -- every response is read generically
// via extractResults, which is what lets a single orchestrator reply render a policy card and a
// claim card side by side, same as it lets a domain agent's reply render just its own one.
const AGENT_CONFIG = {
  orchestrator: {
    label: 'AI Assistant',
    initials: 'AI',
    api: aiAssistantApi,
    placeholder: 'Ask about policies, claims, or renewals...',
    subtitle: () =>
      'Automatically routes your question to the right specialist agent',
    suggestions: (isStaff) =>
      isStaff ? STAFF_ASSISTANT_SUGGESTIONS : CUSTOMER_ASSISTANT_SUGGESTIONS,
  },
  policy: {
    label: 'Policy Agent',
    initials: 'PA',
    api: policyAgentApi,
    placeholder: 'Ask about a policy...',
    subtitle: (isStaff) =>
      isStaff
        ? 'Search, view, create, and update policies'
        : 'Search and view your policies',
    suggestions: (isStaff) =>
      isStaff ? STAFF_POLICY_SUGGESTIONS : CUSTOMER_POLICY_SUGGESTIONS,
  },
  claims: {
    label: 'Claims Agent',
    initials: 'CA',
    api: claimsAgentApi,
    placeholder: 'Ask about a claim...',
    subtitle: (isStaff) =>
      isStaff
        ? 'Search, review, and process claims'
        : 'File and check the status of your claims',
    suggestions: (isStaff) =>
      isStaff ? STAFF_CLAIMS_SUGGESTIONS : CUSTOMER_CLAIMS_SUGGESTIONS,
  },
  renewal: {
    label: 'Renewal Agent',
    initials: 'RA',
    api: renewalAgentApi,
    placeholder: 'Ask about a renewal...',
    subtitle: (isStaff) =>
      isStaff
        ? 'Search, review, and process renewals'
        : 'Request and track your policy renewals',
    suggestions: () => RENEWAL_SUGGESTIONS,
  },
}

function errorMessage(err) {
  return (
    err.response?.data?.message ||
    'Something went wrong talking to the assistant.'
  )
}

// Both agent endpoints are single-turn/stateless by design -- neither has memory of earlier
// messages. To keep follow-up questions coherent without touching the backend, recent turns are
// stitched into the outgoing message as plain-text context. The full reply text is kept in state
// even though the UI only shows a short caption when a card is rendered (see hasStructuredResult
// below), so the agent still gets its own prior answers as context.
function buildPromptWithHistory(history, newMessage) {
  const recent = history
    .slice(-HISTORY_TURNS_FOR_CONTEXT)
    .filter((m) => !m.isError)
  if (recent.length === 0) return newMessage

  const context = recent
    .map((m) => `${m.role === 'user' ? 'User' : 'Assistant'}: ${m.content}`)
    .join('\n')
  return `Previous conversation:\n${context}\n\nNew message: ${newMessage}`
}

function makeId() {
  return crypto.randomUUID()
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
          <span className={styles.resultPrimaryLabel}>
            {policy.policyNumber}
          </span>
          <span className={styles.resultSecondaryLabel}>
            {policy.policyType}
          </span>
        </div>
        <StatusBadge status={policy.status} />
      </div>
      <div className={styles.resultGrid}>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Coverage</span>
          <strong>{formatCurrency(policy.coverageAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Premium</span>
          <strong>{formatCurrency(policy.premiumAmount)}</strong>
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
              <span className={styles.resultPrimaryLabel}>
                {policy.policyNumber}
              </span>
              <span className={styles.resultSecondaryLabel}>
                {policy.policyType}
              </span>
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
          +{remaining} more &mdash; ask to narrow your search (by type, status,
          or customer) to see them.
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
          <span className={styles.resultSecondaryLabel}>
            {claim.policyNumber}
          </span>
        </div>
        <StatusBadge status={claim.status} />
      </div>
      <div className={styles.resultGrid}>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Claim Amount (INR)</span>
          <strong>{formatCurrency(claim.claimAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Approved Amount (INR)</span>
          <strong>
            {claim.approvedAmount != null
              ? formatCurrency(claim.approvedAmount)
              : '—'}
          </strong>
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
              <span className={styles.resultPrimaryLabel}>
                {claim.claimNumber}
              </span>
              <span className={styles.resultSecondaryLabel}>
                {claim.policyNumber}
              </span>
            </div>
            <div className={styles.resultListItemMeta}>
              <span className={styles.resultLabel}>
                {formatCurrency(claim.claimAmount)}
              </span>
              <StatusBadge status={claim.status} />
            </div>
          </div>
        ))}
      </div>
      {remaining > 0 && (
        <p className={styles.resultListFooter}>
          +{remaining} more &mdash; ask to narrow your search (by policy or
          status) to see them.
        </p>
      )}
    </div>
  )
}

// The primary response component for a single renewal lookup/create/decide result. Note: renewals
// carry no "current premium" or "policy type" in the backend model -- those live on the Policy,
// not the Renewal, and the Renewal Agent's response never includes them. Previous End Date and the
// requested/decided timestamp are shown in their place, since those are real fields.
function RenewalSummary({ renewal }) {
  return (
    <div className={styles.resultCard}>
      <div className={styles.resultCardHeader}>
        <div>
          <span className={styles.resultPrimaryLabel}>
            {renewal.policyNumber}
          </span>
        </div>
        <StatusBadge status={renewal.status} />
      </div>
      <div className={styles.resultGrid}>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Renewal Premium</span>
          <strong>{formatCurrency(renewal.revisedPremiumAmount)}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Previous End Date</span>
          <strong>{renewal.previousEndDate}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>Renewal Date</span>
          <strong>{renewal.newEndDate}</strong>
        </div>
        <div className={styles.resultGridItem}>
          <span className={styles.resultLabel}>
            {renewal.decidedAt ? 'Decided' : 'Requested'}
          </span>
          <strong>
            {formatDate(renewal.decidedAt ?? renewal.requestedAt)}
          </strong>
        </div>
      </div>
    </div>
  )
}

// The primary response component for a multi-renewal search result -- same compact card-list
// shape as PolicyListSummary / ClaimListSummary, capped the same way.
function RenewalListSummary({ renewals }) {
  if (renewals.length === 0) return null
  const shown = renewals.slice(0, MAX_RESULTS_SHOWN)
  const remaining = renewals.length - shown.length

  return (
    <div className={styles.resultListWrapper}>
      <div className={styles.resultList}>
        {shown.map((renewal) => (
          <div key={renewal.id} className={styles.resultListItem}>
            <div>
              <span className={styles.resultPrimaryLabel}>
                {renewal.policyNumber}
              </span>
              <span className={styles.resultSecondaryLabel}>
                {renewal.newEndDate}
              </span>
            </div>
            <div className={styles.resultListItemMeta}>
              <span className={styles.resultLabel}>
                {formatCurrency(renewal.revisedPremiumAmount)}
              </span>
              <StatusBadge status={renewal.status} />
            </div>
          </div>
        ))}
      </div>
      {remaining > 0 && (
        <p className={styles.resultListFooter}>
          +{remaining} more &mdash; ask to narrow your search (by policy or
          status) to see them.
        </p>
      )}
    </div>
  )
}

// Dispatches to the right result component for whichever agent is selected -- keeps the render
// logic below from growing a nested ternary per agent as more agents are added.
function renderSingleResult(resultKind, data) {
  switch (resultKind) {
    case 'policy':
      return <PolicySummary policy={data} />
    case 'claim':
      return <ClaimSummary claim={data} />
    case 'renewal':
      return <RenewalSummary renewal={data} />
    default:
      return null
  }
}

function renderListResult(resultKind, data) {
  switch (resultKind) {
    case 'policy':
      return <PolicyListSummary policies={data} />
    case 'claim':
      return <ClaimListSummary claims={data} />
    case 'renewal':
      return <RenewalListSummary renewals={data} />
    default:
      return null
  }
}

const RESULT_KIND_PLURAL = {
  policy: 'policies',
  claim: 'claims',
  renewal: 'renewals',
}

function pluralizeResultKind(kind, count) {
  return count === 1 ? kind : (RESULT_KIND_PLURAL[kind] ?? `${kind}s`)
}

// Reads whichever policy/claim/renewal fields are present on a chat response into a flat list of
// {kind, single, list} entries. A single-domain agent's response only ever has its own domain's
// fields set, so this naturally yields at most one entry for those; the orchestrator's response
// can have more than one domain populated at once, so this is what lets the UI render a policy
// card and a claim card side by side for one combined answer.
function extractResults(data) {
  const results = []
  if (data.policy) results.push({ kind: 'policy', single: data.policy })
  if (data.policies?.length)
    results.push({ kind: 'policy', list: data.policies })
  if (data.claim) results.push({ kind: 'claim', single: data.claim })
  if (data.claims?.length) results.push({ kind: 'claim', list: data.claims })
  if (data.renewal) results.push({ kind: 'renewal', single: data.renewal })
  if (data.renewals?.length)
    results.push({ kind: 'renewal', list: data.renewals })
  return results
}

// Shown below a Knowledge Agent (RAG) answer -- just the document name each retrieved passage
// came from, as a row of compact chips. Deliberately shows nothing else from the response's
// `chunks` (no passage text, page number, similarity score, or tool internals) -- that data
// exists for future use, not for display here. Renders nothing when there are no sources, so it
// never appears on a Policy/Claims/Renewal answer.
function SourcesSection({ sources }) {
  if (!sources?.length) return null

  return (
    <div className={styles.sourcesSection}>
      <span className={styles.sourcesLabel}>Sources</span>
      <div className={styles.sourcesList}>
        {sources.map((source) => (
          <span key={source} className={styles.sourceChip}>
            <DocumentIcon
              className={styles.sourceChipIcon}
              width={14}
              height={14}
            />
            {source}
          </span>
        ))}
      </div>
    </div>
  )
}

// Persist conversation text only with explicit consent: document contents and result DTOs
// never enter localStorage by default. Session metadata remains available to navigation.
export function ChatPage() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const username = user?.username
  const storageKey = username ? `insuranceai.chats.${username}` : null
  const routeId = params.get('chat')
  const promptParam = params.get('prompt')
  const [selectedAgent, setSelectedAgent] = useState('orchestrator')
  const [messages, setMessages] = useState([])
  const [historyOwner, setHistoryOwner] = useState(username)
  const [input, setInput] = useState('')
  const [sessionId, setSessionId] = useState(null)
  const [isSending, setIsSending] = useState(false)
  const [failure, setFailure] = useState(null)
  const [storageError, setStorageError] = useState('')
  const [toast, setToast] = useState('')
  const [saveText, setSaveText] = useState(false)
  const [showBottom, setShowBottom] = useState(false)
  const mounted = useRef(false)
  const generation = useRef(0)
  const pending = useRef(false)
  const currentId = useRef(null)
  const currentUser = useRef(username)
  const messageListRef = useRef(null)
  const textareaRef = useRef(null)
  const agent = AGENT_CONFIG[selectedAgent]
  const isStaff = STAFF_ROLES.has(user?.role)
  const suggestions =
    selectedAgent === 'orchestrator'
      ? [
          'Explain my insurance policy.',
          'What does my policy cover?',
          'How do I file a claim?',
          'Help me calculate my premium.',
          'Compare insurance plans.',
        ]
      : agent.suggestions(isStaff)

  useEffect(() => {
    mounted.current = true
    return () => {
      mounted.current = false
      generation.current += 1
      pending.current = false
    }
  }, [])

  useEffect(() => {
    if (currentId.current === routeId && currentUser.current === username) {
      // Route query parameters are an external source of draft state.
      // oxlint-disable-next-line react/set-state-in-effect
      if (promptParam) setInput(promptParam)
      return
    }
    generation.current += 1
    pending.current = false
    setIsSending(false)
    setFailure(null)
    setSaveText(false)
    currentUser.current = username
    setHistoryOwner(username)
    let session
    if (storageKey && routeId) {
      try {
        const saved = JSON.parse(localStorage.getItem(storageKey) || '[]')
        session = Array.isArray(saved)
          ? saved.find((item) => item.id === routeId)
          : null
      } catch {
        setStorageError(
          'Saved conversations could not be read. You can still chat.',
        )
      }
    }
    currentId.current = routeId
    setSessionId(routeId)
    setSelectedAgent(
      AGENT_CONFIG[session?.agent] ? session.agent : 'orchestrator',
    )
    setMessages(
      Array.isArray(session?.messages)
        ? session.messages.filter(
            (m) =>
              ['user', 'assistant'].includes(m.role) &&
              typeof m.content === 'string',
          )
        : [],
    )
    setSaveText(Boolean(session?.messages?.length))
    setInput(promptParam || '')
  }, [routeId, promptParam, username, storageKey])

  useEffect(() => {
    if (
      historyOwner !== username ||
      !storageKey ||
      !sessionId ||
      !messages.length ||
      currentId.current !== sessionId ||
      currentUser.current !== username
    )
      return
    try {
      const raw = JSON.parse(localStorage.getItem(storageKey) || '[]')
      const sessions = Array.isArray(raw) ? raw : []
      const first = messages.find((m) => m.role === 'user')
      const entry = {
        id: sessionId,
        title: saveText
          ? first?.content.slice(0, 64) || 'Conversation'
          : 'Private conversation',
        updatedAt: new Date().toISOString(),
        agent: selectedAgent,
        messages: saveText
          ? messages.map(
              ({ id, role, content, createdAt, isError, sources }) => ({
                id,
                role,
                content: sources?.length
                  ? '[Document-based response omitted from device history. Ask again to retrieve it.]'
                  : content,
                createdAt,
                isError,
              }),
            )
          : [],
      }
      localStorage.setItem(
        storageKey,
        JSON.stringify(
          [entry, ...sessions.filter((s) => s.id !== sessionId)].slice(0, 50),
        ),
      )
      window.dispatchEvent(new Event('insuranceai:chats-changed'))
    } catch {
      // Surface failure while synchronizing with external browser storage.
      // oxlint-disable-next-line react/set-state-in-effect
      setStorageError(
        'Conversation could not be saved on this device. Your current chat is still available here.',
      )
    }
  }, [
    messages,
    selectedAgent,
    sessionId,
    storageKey,
    username,
    saveText,
    historyOwner,
  ])

  useEffect(() => {
    const el = messageListRef.current
    if (el && (messages.length || isSending)) el.scrollTop = el.scrollHeight
  }, [messages, isSending])
  useEffect(() => {
    const el = textareaRef.current
    if (el) {
      el.style.height = 'auto'
      el.style.height = `${Math.min(el.scrollHeight, MAX_TEXTAREA_HEIGHT)}px`
    }
  }, [input])
  useEffect(() => {
    if (!toast) return
    const timer = setTimeout(() => setToast(''), 4500)
    return () => clearTimeout(timer)
  }, [toast])

  async function sendToAgent(text, history, agentKey = selectedAgent) {
    if (pending.current) return
    pending.current = true
    setIsSending(true)
    const token = ++generation.current
    const valid = () => mounted.current && generation.current === token
    try {
      const data = await AGENT_CONFIG[agentKey].api.chat({
        message: buildPromptWithHistory(history, text),
      })
      if (!valid()) return
      setMessages((prev) => [
        ...prev,
        {
          id: makeId(),
          role: 'assistant',
          content: data.reply || 'No response was returned.',
          results: extractResults(data),
          sources: Array.isArray(data.sources) ? data.sources : [],
          createdAt: new Date().toISOString(),
        },
      ])
      setFailure(null)
    } catch (err) {
      if (!valid()) return
      const id = makeId()
      setMessages((prev) => [
        ...prev,
        {
          id,
          role: 'assistant',
          content: errorMessage(err),
          isError: true,
          createdAt: new Date().toISOString(),
        },
      ])
      setFailure({ id, text, history, agentKey })
    } finally {
      if (valid()) {
        pending.current = false
        setIsSending(false)
      }
    }
  }
  function submit(raw = input) {
    const text = raw.trim()
    if (!text || pending.current) return
    if (!sessionId) {
      const id = makeId()
      currentId.current = id
      setSessionId(id)
      setParams({ chat: id }, { replace: true })
    }
    const history = messages
    setMessages((prev) => [
      ...prev,
      {
        id: makeId(),
        role: 'user',
        content: text,
        createdAt: new Date().toISOString(),
      },
    ])
    setInput('')
    setFailure(null)
    sendToAgent(text, history)
  }
  function newChat(agentKey = selectedAgent) {
    if (pending.current) return
    const id = makeId()
    currentId.current = id
    setSessionId(id)
    setMessages([])
    setFailure(null)
    setSaveText(false)
    setInput('')
    setToast('')
    setSelectedAgent(agentKey)
    setParams({ chat: id }, { replace: true })
  }
  function clearChat() {
    if (pending.current) return
    try {
      if (storageKey && sessionId) {
        const saved = JSON.parse(localStorage.getItem(storageKey) || '[]')
        localStorage.setItem(
          storageKey,
          JSON.stringify(
            Array.isArray(saved) ? saved.filter((s) => s.id !== sessionId) : [],
          ),
        )
        window.dispatchEvent(new Event('insuranceai:chats-changed'))
      }
    } catch {
      setStorageError(
        'Saved conversation could not be removed from this device.',
      )
    }
    newChat()
  }
  async function copy(message) {
    try {
      await navigator.clipboard.writeText(message.content)
      if (mounted.current) setToast('Response copied.')
    } catch {
      if (mounted.current)
        setToast(
          'Could not copy. Select the response text to copy it manually.',
        )
    }
  }
  async function regenerate(message) {
    if (pending.current || !message.sources?.length) return
    const index = messages.findIndex((m) => m.id === message.id)
    const questionIndex = messages
      .slice(0, index)
      .findLastIndex((m) => m.role === 'user')
    if (questionIndex < 0) return
    pending.current = true
    setIsSending(true)
    const token = ++generation.current
    try {
      const data = await knowledgeAgentApi.chat({
        message: buildPromptWithHistory(
          messages.slice(0, questionIndex),
          messages[questionIndex].content,
        ),
      })
      if (!mounted.current || generation.current !== token) return
      setMessages((old) =>
        old.map((m) =>
          m.id === message.id
            ? {
                ...m,
                content: data.reply || 'No response returned.',
                sources: data.sources || [],
                createdAt: new Date().toISOString(),
              }
            : m,
        ),
      )
      setToast('Answer regenerated from the knowledge library.')
    } catch {
      if (mounted.current && generation.current === token)
        setToast(
          'Could not regenerate. Your previous answer is still available.',
        )
    } finally {
      if (mounted.current && generation.current === token) {
        pending.current = false
        setIsSending(false)
      }
    }
  }
  const lastAssistant = messages.findLast(
    (m) => m.role === 'assistant' && !m.isError,
  )

  return (
    <section
      className={styles.chatWindow}
      aria-label="Insurance AI conversation"
    >
      <header className={styles.chatHeader}>
        <div className={styles.agentIdentity}>
          <div className={styles.agentAvatar}>
            <Sparkles size={22} aria-hidden="true" />
          </div>
          <div>
            <div className={styles.agentName}>Insurance AI Assistant</div>
            <div className={styles.agentSubtitle}>
              <span className={styles.onlineDot} />
              Online - Your personal insurance guide
            </div>
          </div>
        </div>
        <div className={styles.headerActions}>
          <button type="button" onClick={() => newChat()} disabled={isSending}>
            <Plus size={16} /> New chat
          </button>
          <button
            type="button"
            onClick={clearChat}
            disabled={isSending || !messages.length}
            aria-label="Clear conversation"
          >
            <Trash2 size={17} />
          </button>
        </div>
      </header>
      <div
        className={styles.agentSwitcher}
        role="group"
        aria-label="Choose an agent; switching starts a new conversation"
      >
        {Object.entries(AGENT_CONFIG).map(([key, config]) => (
          <button
            key={key}
            type="button"
            aria-pressed={selectedAgent === key}
            className={`${styles.agentTab} ${selectedAgent === key ? styles.agentTabActive : ''}`}
            disabled={isSending}
            onClick={() => key !== selectedAgent && newChat(key)}
          >
            {config.label}
          </button>
        ))}
      </div>
      <p className={styles.permissionNote}>
        {agent.subtitle(isStaff)}. Your account permissions apply.
      </p>
      {storageError && (
        <p role="alert" className={styles.storageError}>
          {storageError}
        </p>
      )}
      <div
        className={styles.messageList}
        ref={messageListRef}
        role="log"
        aria-label="Conversation messages"
        aria-live="polite"
        aria-busy={isSending}
        onScroll={(e) => {
          const el = e.currentTarget
          setShowBottom(el.scrollHeight - el.scrollTop - el.clientHeight > 100)
        }}
      >
        {!messages.length && (
          <div className={styles.emptyState}>
            <div className={styles.welcomeIcon}>
              <Sparkles size={32} aria-hidden="true" />
            </div>

            <h1 className={styles.emptyTitle}>
              Hello! I'm your Insurance AI Assistant.
            </h1>
            <p className={styles.emptyMessage}>
              I'm here to help you understand insurance policies, compare
              coverage, calculate premiums, and navigate claims.
            </p>
            <div className={styles.suggestions}>
              {suggestions.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  className={styles.suggestionChip}
                  onClick={() => {
                    setInput(suggestion)
                    textareaRef.current?.focus()
                  }}
                >
                  {suggestion}
                  <ArrowUp size={16} aria-hidden="true" />
                </button>
              ))}
            </div>
          </div>
        )}
        {messages.map((message) => (
          <article
            key={message.id}
            className={`${styles.messageRow} ${message.role === 'user' ? styles.rowUser : styles.rowAssistant}`}
            aria-label={
              message.role === 'user' ? 'You' : 'Insurance AI Assistant'
            }
          >
            {message.role === 'assistant' && (
              <div className={styles.messageAvatar}>
                <Sparkles size={16} aria-hidden="true" />
              </div>
            )}
            <div className={styles.messageColumn}>
              <div
                className={`${styles.bubble} ${message.role === 'user' ? styles.bubbleUser : message.isError ? styles.bubbleError : styles.bubbleAssistant}`}
              >
                {message.role === 'user' || message.isError ? (
                  message.content
                ) : (
                  <ReactMarkdown
                    remarkPlugins={[remarkGfm]}
                    skipHtml
                    components={{
                      img: () => null,
                      a: ({ children, href }) => (
                        <a
                          href={href}
                          target="_blank"
                          rel="noopener noreferrer"
                        >
                          {children}
                        </a>
                      ),
                    }}
                  >
                    {message.content}
                  </ReactMarkdown>
                )}
              </div>
              {(message.results || []).map((result, index) => (
                <div key={`${result.kind}-${index}`}>
                  <p className={styles.resultCaption}>
                    {result.single
                      ? `Your ${result.kind} details`
                      : `${result.list.length} matching ${pluralizeResultKind(result.kind, result.list.length)}`}
                  </p>
                  {result.single
                    ? renderSingleResult(result.kind, result.single)
                    : renderListResult(result.kind, result.list)}
                </div>
              ))}
              <SourcesSection sources={message.sources} />
              <div className={styles.messageMeta}>
                {message.createdAt && (
                  <time dateTime={message.createdAt}>
                    {new Date(message.createdAt).toLocaleTimeString([], {
                      hour: '2-digit',
                      minute: '2-digit',
                    })}
                  </time>
                )}
                {message.role === 'assistant' && !message.isError && (
                  <button
                    type="button"
                    onClick={() => copy(message)}
                    aria-label="Copy response"
                  >
                    <Copy size={14} /> Copy
                  </button>
                )}
                {message.id === lastAssistant?.id &&
                  Boolean(message.sources?.length) && (
                    <button
                      type="button"
                      disabled={isSending}
                      onClick={() => regenerate(message)}
                    >
                      <RotateCcw size={14} /> Regenerate
                    </button>
                  )}
              </div>
              {message.isError && failure?.id === message.id && (
                <div className={styles.retryArea}>
                  <p>
                    The request may have completed. Check your records before
                    retrying an action.
                  </p>
                  <button
                    type="button"
                    className={styles.retryButton}
                    disabled={isSending}
                    onClick={() => {
                      if (!pending.current) {
                        setFailure(null)
                        sendToAgent(
                          failure.text,
                          failure.history,
                          failure.agentKey,
                        )
                      }
                    }}
                  >
                    Retry last request
                  </button>
                </div>
              )}
            </div>
          </article>
        ))}
        {isSending && (
          <div className={styles.thinking} role="status">
            <Sparkles size={18} aria-hidden="true" />
            <span className={styles.typingDot} /> {agent.label} is thinking...
          </div>
        )}
      </div>
      {showBottom && (
        <button
          type="button"
          className={styles.scrollBottom}
          onClick={() =>
            messageListRef.current?.scrollTo({
              top: messageListRef.current.scrollHeight,
              behavior: 'smooth',
            })
          }
        >
          <ArrowDown size={16} /> Latest messages
        </button>
      )}
      <footer className={styles.composerFooter}>
        <div className={styles.composer}>
          <Link
            className={styles.attachButton}
            to="/documents"
            aria-label="Open document library"
            title="Manage documents in the document library"
          >
            <Paperclip size={20} />
          </Link>
          <textarea
            ref={textareaRef}
            className={styles.textarea}
            disabled={isSending}
            aria-label="Message your insurance assistant"
            placeholder={agent.placeholder}
            rows={1}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={(e) => {
              if (
                e.key === 'Enter' &&
                !e.shiftKey &&
                !e.nativeEvent.isComposing &&
                e.keyCode !== 229
              ) {
                e.preventDefault()
                submit()
              }
            }}
          />
          <button
            type="button"
            className={styles.sendButton}
            onClick={() => submit()}
            disabled={isSending || !input.trim()}
            aria-label="Send message"
          >
            <ArrowUp size={20} />
          </button>
        </div>
        <div className={styles.composerNotes}>
          <span>Enter to send - Shift+Enter for a new line</span>
          <Link to="/documents">Manage documents</Link>
        </div>
        <label className={styles.historyConsent}>
          <input
            type="checkbox"
            checked={saveText}
            disabled={isSending}
            onChange={(e) => setSaveText(e.target.checked)}
          />{' '}
          Save chat text on this device. Leave off for sensitive information or
          document content.
        </label>
        <p className={styles.hint}>
          AI can make mistakes. Verify important details in your policy
          documents.
        </p>
      </footer>
      <div
        className={styles.toast}
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        {toast}
      </div>
    </section>
  )
}
