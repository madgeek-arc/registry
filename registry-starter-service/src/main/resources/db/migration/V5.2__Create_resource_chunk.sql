CREATE EXTENSION IF NOT EXISTS vector;

DO $$
BEGIN
    IF to_regclass('public.resource') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS resource_chunk
        (
            id              bigserial PRIMARY KEY,
            resource_id     text         NOT NULL,
            chunk_idx       int          NOT NULL,
            field_name      varchar(255) NOT NULL,
            value_ordinal   int          NOT NULL DEFAULT 0,
            field_chunk_idx int          NOT NULL DEFAULT 0,
            content         text         NOT NULL,
            embedding       vector(384)  NOT NULL,
            embedding_model varchar(255) NOT NULL,
            created_at      timestamptz  NOT NULL DEFAULT now(),

            UNIQUE (resource_id, chunk_idx)
        );

        CREATE INDEX IF NOT EXISTS resource_chunk_embedding_hnsw
            ON resource_chunk
            USING hnsw (embedding vector_cosine_ops);

        CREATE INDEX IF NOT EXISTS resource_chunk_resource_id_idx ON resource_chunk (resource_id);
        CREATE INDEX IF NOT EXISTS resource_chunk_embedding_model_idx ON resource_chunk (embedding_model);
        CREATE INDEX IF NOT EXISTS resource_chunk_field_name_idx ON resource_chunk (field_name);

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_chunk_resource'
        ) THEN
            ALTER TABLE resource_chunk
                ADD CONSTRAINT fk_chunk_resource
                    FOREIGN KEY (resource_id) REFERENCES resource (id)
                        ON DELETE CASCADE;
        END IF;
    END IF;
END $$;
