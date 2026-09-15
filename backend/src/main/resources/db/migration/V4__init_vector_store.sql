-- Backs the local RAG pipeline's VectorStore (Spring AI's PgVectorStore). Owned by Flyway, like
-- every other table in this project (spring.jpa.hibernate.ddl-auto=validate and
-- spring.ai.vectorstore.pgvector.initialize-schema=false both rely on that) -- this table is
-- deliberately not JPA-mapped, so Hibernate's schema validation never looks at it.
--
-- The shape below is exactly what PgVectorStore itself would create if initializeSchema were
-- true (same table/column/index names, same defaults), so this stays a drop-in match for the
-- library's own expectations.

CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata json,
    embedding vector(1536)
);

CREATE INDEX IF NOT EXISTS spring_ai_vector_index ON vector_store USING hnsw (embedding vector_cosine_ops);
