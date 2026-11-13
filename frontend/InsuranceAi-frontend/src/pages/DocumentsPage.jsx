import { useEffect, useRef, useState } from 'react'
import { useAuth } from '../hooks/useAuth'
import {
  DOCUMENT_CATEGORIES,
  deleteDocument,
  listDocuments,
  saveDocument,
} from '../api/localWorkspace'
import styles from './WorkspacePages.module.css'

export function DocumentsPage() {
  const { user } = useAuth()
  const username = user?.username
  return (
    <DocumentsWorkspace key={username || 'signed-out'} username={username} />
  )
}

function DocumentsWorkspace({ username }) {
  const [documents, setDocuments] = useState([])
  const [category, setCategory] = useState('Policy')
  const [filter, setFilter] = useState('All')
  const [loading, setLoading] = useState(Boolean(username))
  const [progress, setProgress] = useState({ done: 0, total: 0 })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [status, setStatus] = useState('')
  const [drag, setDrag] = useState(false)
  const [preview, setPreview] = useState(null)
  const [pendingDelete, setPendingDelete] = useState(null)
  const lock = useRef(false)
  const input = useRef(null)
  useEffect(() => {
    let active = true
    if (!username) return
    listDocuments(username)
      .then((items) => {
        if (active) setDocuments(items)
      })
      .catch((e) => {
        if (active) setError(e.message)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [username])
  useEffect(() => {
    return () => {
      if (preview) URL.revokeObjectURL(preview.url)
    }
  }, [preview])
  async function upload(files) {
    if (lock.current || loading || !username || !files.length) return
    lock.current = true
    setBusy(true)
    setError('')
    setStatus('')
    setProgress({ done: 0, total: files.length })
    const errors = []
    let count = 0
    let completed = 0
    for (const file of files) {
      try {
        const item = await saveDocument(username, file, category)
        setDocuments((old) => [item, ...old])
        count++
      } catch (e) {
        errors.push(e.message)
      }
      completed++
      setProgress({ done: completed, total: files.length })
    }
    setError(errors.join(' '))
    setStatus(`${count} PDF${count === 1 ? '' : 's'} saved locally.`)
    lock.current = false
    setBusy(false)
    if (input.current) input.current.value = ''
  }
  async function remove(item) {
    if (lock.current) return
    lock.current = true
    setBusy(true)
    setError('')
    try {
      await deleteDocument(username, item.id)
      setDocuments((old) => old.filter((d) => d.id !== item.id))
      if (preview?.id === item.id) setPreview(null)
      setPendingDelete(null)
      setStatus(`${item.name} deleted from this browser.`)
    } catch (e) {
      setError(e.message)
    } finally {
      lock.current = false
      setBusy(false)
    }
  }
  function download(item) {
    const url = URL.createObjectURL(item.blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = item.name
    anchor.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }
  const visible = documents.filter(
    (d) => filter === 'All' || d.category === filter,
  )
  return (
    <section className={styles.page} aria-labelledby="documents-title">
      <h1 id="documents-title">Documents</h1>
      <p className={styles.intro}>
        Keep policy and claim PDFs together on this device. Files stay in your
        browser; they are not uploaded, submitted to an insurer or available to
        the AI assistant.
      </p>
      <div className={styles.notice}>
        Local only · PDFs up to 20 MB each. Clearing browser site data removes
        these files. Keep a separate backup.
      </div>
      {!username ? (
        <p>Sign in to access your local documents.</p>
      ) : (
        <>
          <div className={styles.grid}>
            <div
              className={`${styles.drop} ${drag ? styles.drag : ''}`}
              onDragOver={(e) => {
                e.preventDefault()
                setDrag(true)
              }}
              onDragLeave={() => setDrag(false)}
              onDrop={(e) => {
                e.preventDefault()
                setDrag(false)
                upload(Array.from(e.dataTransfer.files))
              }}
              aria-busy={busy}
            >
              <h2>Add PDF documents</h2>
              <p className={styles.muted}>
                Drag files here or choose them below.
              </p>
              <input
                ref={input}
                type="file"
                accept="application/pdf,.pdf"
                multiple
                aria-label="Choose PDF documents"
                disabled={busy || loading}
                onChange={(e) => upload(Array.from(e.target.files))}
              />
            </div>
            <div className={`${styles.card} ${styles.stack}`}>
              <label className={styles.field}>
                Category for new files
                <select
                  value={category}
                  disabled={busy}
                  onChange={(e) => setCategory(e.target.value)}
                >
                  {DOCUMENT_CATEGORIES.map((c) => (
                    <option key={c}>{c}</option>
                  ))}
                </select>
              </label>
              <p className={styles.muted}>
                Choose a category to keep your policy, identity, and claim
                documents organized.
              </p>
            </div>
          </div>
          {error && (
            <p role="alert" className={styles.error}>
              {error}
            </p>
          )}
          <p role="status" aria-live="polite" className={styles.muted}>
            {busy
              ? `Saving files: ${progress.done} of ${progress.total} completed`
              : status}
          </p>
          <>
            {busy && (
              <progress
                aria-label="Local file save progress"
                value={progress.done}
                max={progress.total || 1}
                style={{ width: '100%' }}
              />
            )}
          </>
          <label className={styles.field}>
            Filter by category
            <select value={filter} onChange={(e) => setFilter(e.target.value)}>
              {['All', ...DOCUMENT_CATEGORIES].map((c) => (
                <option key={c}>{c}</option>
              ))}
            </select>
          </label>
          {loading ? (
            <p role="status">Loading local documents…</p>
          ) : !visible.length ? (
            <div className={styles.empty}>
              <h2>
                {documents.length
                  ? 'No PDFs in this category'
                  : 'No documents saved yet'}
              </h2>
              <p className={styles.muted}>
                Choose a PDF to start your local collection.
              </p>
            </div>
          ) : (
            <ul className={styles.list}>
              {visible.map((item) => (
                <li key={item.id} className={styles.card}>
                  <div className={styles.row}>
                    <div>
                      <p className={styles.name}>{item.name}</p>
                      <p className={styles.muted}>
                        {item.category} · {(item.size / 1024 / 1024).toFixed(2)}{' '}
                        MB · {new Date(item.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className={styles.actions}>
                      <button
                        type="button"
                        disabled={busy}
                        aria-label={`Preview ${item.name}`}
                        onClick={() =>
                          setPreview({
                            id: item.id,
                            name: item.name,
                            url: URL.createObjectURL(item.blob),
                          })
                        }
                      >
                        Preview
                      </button>
                      <button
                        type="button"
                        aria-label={`Download ${item.name}`}
                        onClick={() => download(item)}
                      >
                        Download
                      </button>
                      <button
                        type="button"
                        disabled={busy}
                        aria-label={`Delete ${item.name}`}
                        onClick={() => setPendingDelete(item.id)}
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                  {pendingDelete === item.id && (
                    <div className={styles.notice}>
                      <p>
                        Delete {item.name} from this browser? This cannot be
                        undone.
                      </p>
                      <div className={styles.actions}>
                        <button
                          disabled={busy}
                          type="button"
                          onClick={() => remove(item)}
                        >
                          Confirm delete
                        </button>
                        <button
                          disabled={busy}
                          type="button"
                          onClick={() => setPendingDelete(null)}
                        >
                          Cancel
                        </button>
                      </div>
                    </div>
                  )}
                </li>
              ))}
            </ul>
          )}
          {preview && (
            <section className={styles.card} aria-label="PDF preview">
              <div className={styles.row}>
                <h2>{preview.name}</h2>
                <button type="button" onClick={() => setPreview(null)}>
                  Close preview
                </button>
              </div>
              <p className={styles.muted}>
                If your browser cannot display PDFs, use Download to open the
                file in a PDF reader.
              </p>
              <iframe
                className={styles.preview}
                title={`PDF preview: ${preview.name}`}
                src={preview.url}
              />
            </section>
          )}
        </>
      )}
    </section>
  )
}
