-- ============================================================
-- ZAgent PgVector Schema
-- ============================================================

CREATE EXTENSION IF NOT EXISTS vector;

DROP TABLE IF EXISTS public.vector_store;
CREATE TABLE public.vector_store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content TEXT NOT NULL,
    metadata JSONB,
    embedding VECTOR(1536)
);

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding ON public.vector_store USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
