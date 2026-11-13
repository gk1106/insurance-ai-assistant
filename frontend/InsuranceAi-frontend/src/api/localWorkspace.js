// Browser-only workspace. Username scoping is organizational, not encryption.
export const CHAT_CHANGE_EVENT = 'insuranceai:chats-changed'
export const chatStorageKey = (username) => {
  if (!username) throw new Error('Sign in to access your local workspace.')
  return `insuranceai.chats.${username}`
}

export function readChats(username) {
  const value = JSON.parse(
    localStorage.getItem(chatStorageKey(username)) || '[]',
  )
  if (!Array.isArray(value))
    throw new Error('Saved chat history has an unsupported format.')
  return value
    .filter((s) => s && typeof s.id === 'string')
    .map((s) => ({
      ...s,
      title: typeof s.title === 'string' ? s.title : 'Untitled conversation',
      agent: typeof s.agent === 'string' ? s.agent : 'orchestrator',
      messages: Array.isArray(s.messages)
        ? s.messages.filter((m) => m && typeof m.content === 'string')
        : [],
    }))
    .sort(
      (a, b) => (Date.parse(b.updatedAt) || 0) - (Date.parse(a.updatedAt) || 0),
    )
}

export const DOCUMENT_CATEGORIES = ['Policy', 'Claim', 'Identity', 'Other']
export const MAX_PDF_BYTES = 20 * 1024 * 1024
async function database() {
  if (!globalThis.indexedDB)
    throw new Error('This browser does not support local document storage.')
  return new Promise((resolve, reject) => {
    const request = indexedDB.open('insuranceai.workspace', 1)
    request.onupgradeneeded = () =>
      request.result.createObjectStore('documents', { keyPath: 'id' })
    request.onsuccess = () => resolve(request.result)
    request.onerror = () =>
      reject(
        new Error(
          'Local storage could not be opened. Check browser storage permissions.',
        ),
      )
    request.onblocked = () =>
      reject(new Error('Close other InsuranceAI tabs and try again.'))
  })
}
async function transaction(mode, action) {
  const db = await database()
  return new Promise((resolve, reject) => {
    const tx = db.transaction('documents', mode)
    let result
    const request = action(tx.objectStore('documents'))
    request.onsuccess = () => {
      result = request.result
    }
    tx.oncomplete = () => {
      db.close()
      resolve(result)
    }
    tx.onabort = tx.onerror = () => {
      db.close()
      reject(
        new Error(
          'Could not save or read documents. Storage may be full or unavailable.',
        ),
      )
    }
  })
}
function requireUser(username) {
  chatStorageKey(username)
  return username
}
export async function listDocuments(username) {
  requireUser(username)
  return (await transaction('readonly', (store) => store.getAll()))
    .filter((d) => d.username === username)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
}
export async function saveDocument(username, file, category) {
  requireUser(username)
  if (!DOCUMENT_CATEGORIES.includes(category))
    throw new Error('Choose a document category.')
  if (
    !/\.pdf$/i.test(file.name) ||
    (file.type && file.type !== 'application/pdf')
  )
    throw new Error(`${file.name}: choose a PDF file.`)
  if (file.size === 0 || file.size > MAX_PDF_BYTES)
    throw new Error(`${file.name}: PDF must be between 1 byte and 20 MB.`)
  const signature = new TextDecoder().decode(
    await file.slice(0, 5).arrayBuffer(),
  )
  if (signature !== '%PDF-')
    throw new Error(`${file.name}: this file does not have a valid PDF header.`)
  const record = {
    id: crypto.randomUUID(),
    username,
    name: file.name,
    category,
    size: file.size,
    createdAt: new Date().toISOString(),
    blob: new Blob([file], { type: 'application/pdf' }),
  }
  await transaction('readwrite', (store) => store.put(record))
  return record
}
export async function deleteDocument(username, id) {
  requireUser(username)
  const record = await transaction('readonly', (store) => store.get(id))
  if (!record || record.username !== username)
    throw new Error('Document not found in your workspace.')
  await transaction('readwrite', (store) => store.delete(id))
}
