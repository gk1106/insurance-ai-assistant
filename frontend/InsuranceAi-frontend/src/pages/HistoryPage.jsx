import { Link } from 'react-router-dom'
import { useEffect, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import {
  CHAT_CHANGE_EVENT,
  chatStorageKey,
  readChats,
} from '../api/localWorkspace'
import styles from './WorkspacePages.module.css'

export function HistoryPage() {
  const { user } = useAuth()
  const username = user?.username
  const [data, setData] = useState({ username: null, chats: [], error: '' })
  const [query, setQuery] = useState('')
  useEffect(() => {
    function refresh() {
      try {
        setData({
          username,
          chats: username ? readChats(username) : [],
          error: '',
        })
      } catch (err) {
        setData({
          username,
          chats: [],
          error: `Could not read local chat history. ${err.message}`,
        })
      }
    }
    function storage(event) {
      if (!event.key || (username && event.key === chatStorageKey(username)))
        refresh()
    }
    refresh()
    window.addEventListener('storage', storage)
    window.addEventListener(CHAT_CHANGE_EVENT, refresh)
    return () => {
      window.removeEventListener('storage', storage)
      window.removeEventListener(CHAT_CHANGE_EVENT, refresh)
    }
  }, [username])
  const chats = data.username === username ? data.chats : []
  const filtered = chats.filter((s) =>
    `${s.title} ${s.agent} ${s.messages.map((m) => m.content).join(' ')}`
      .toLowerCase()
      .includes(query.toLowerCase()),
  )
  return (
    <section className={styles.page} aria-labelledby="history-title">
      <h1 id="history-title">Chat history</h1>
      <p className={styles.intro}>
        Revisit conversations saved for your username in this browser. History
        is local only and does not sync between devices.
      </p>
      <label className={styles.field}>
        Search conversations
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by title, agent or message"
        />
      </label>
      {data.username === username && data.error && (
        <p role="alert" className={styles.error}>
          {data.error}
        </p>
      )}
      {!username ? (
        <p className={styles.notice}>
          Sign in to view your saved conversations.
        </p>
      ) : !filtered.length ? (
        <div className={styles.empty}>
          <h2>
            {query ? 'No matching conversations' : 'No saved conversations yet'}
          </h2>
          <p className={styles.muted}>
            {query
              ? 'Try another search.'
              : 'Conversations appear here when the assistant saves them on this device.'}
          </p>
        </div>
      ) : (
        <>
          <p className={styles.muted} role="status">
            {filtered.length} conversation{filtered.length === 1 ? '' : 's'}
          </p>
          <ul className={styles.list}>
            {filtered.map((session) => (
              <li key={session.id} className={styles.card}>
                <div className={styles.row}>
                  <div>
                    <strong>{session.title}</strong>
                    <p className={styles.muted}>
                      {session.agent} &middot;{' '}
                      {new Date(session.updatedAt).toLocaleString()} &middot;{' '}
                      {session.messages.length} saved messages
                    </p>
                  </div>
                  <Link
                    className={styles.link}
                    to={`/assistant?chat=${encodeURIComponent(session.id)}`}
                  >
                    Open conversation
                  </Link>
                </div>
                <details>
                  <summary>Preview saved messages</summary>
                  {session.messages.length ? (
                    session.messages.map((m, i) => (
                      <div className={styles.message} key={`${m.id || i}-${i}`}>
                        <strong>
                          {m.role === 'user'
                            ? 'You'
                            : m.role === 'assistant'
                              ? 'Assistant'
                              : 'System'}
                        </strong>
                        <p>{m.content}</p>
                      </div>
                    ))
                  ) : (
                    <p className={styles.muted}>
                      No messages saved in this conversation.
                    </p>
                  )}
                </details>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
