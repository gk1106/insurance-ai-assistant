# RAG documents

Drop insurance knowledge PDFs (policy handbooks, FAQs, coverage guides, claims-process docs,
etc.) into this directory, then trigger ingestion:

```
POST /api/ai/rag/ingest
```
(ADMIN only, no request body.)

Each PDF is read page-by-page, split into ~800-token chunks, embedded with the OpenAI embedding
model configured in `application.yml`, and stored in the local `vector_store` Postgres table.
Re-running ingestion after editing a file replaces that file's chunks rather than duplicating
them (matched by file name).

To search what's been ingested:

```
GET /api/ai/rag/search?query=<your question>&topK=5
```
(ADMIN only.)

PDFs placed here are not committed to git (see `.gitignore`) -- only this README is tracked, so
the directory exists in a fresh checkout.

When running via `docker-compose`, this directory is mounted into the backend container at
`/app/documents`, so files added here on the host are visible to the running app without a
rebuild.
