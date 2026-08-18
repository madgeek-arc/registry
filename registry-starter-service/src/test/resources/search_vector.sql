-- Mirrors V6.0__Add_lexical_search_vector.sql. Test schema is built by Hibernate
-- (hbm2ddl.auto=create-drop, Flyway disabled), so the generated column the production
-- migration adds never exists unless this script adds it after Hibernate creates the table.
ALTER TABLE public.resource
    ADD COLUMN IF NOT EXISTS search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('english', payload)) STORED;

CREATE INDEX IF NOT EXISTS resource_search_vector_gin
    ON public.resource USING GIN (search_vector);
