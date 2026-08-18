-- Persisted, indexed lexical search vector backing the "full-text" LexicalSearchStrategy.
-- Lives on the base resource table (not the generated <resourceType>_view views, which
-- are plain views recreated by ViewDaoImpl and cannot host a generated column).
--
-- This is a blocking migration: PostgreSQL rewrites the entire resource table to compute the
-- generated column for every existing row. Accepted as-is for this project's current scale.
ALTER TABLE public.resource
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english', payload)) STORED;

CREATE INDEX IF NOT EXISTS resource_search_vector_gin
    ON public.resource USING GIN (search_vector);
