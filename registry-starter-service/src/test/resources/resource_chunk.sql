CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS resource_chunk
(
    id              bigserial PRIMARY KEY,
    resource_id     varchar(255) NOT NULL,
    chunk_idx       int          NOT NULL,
    field_name      varchar(255) NOT NULL,
    value_ordinal   int          NOT NULL DEFAULT 0,
    field_chunk_idx int          NOT NULL DEFAULT 0,
    content         text         NOT NULL,
    embedding       vector(384)  NOT NULL,
    embedding_model varchar(255) NOT NULL,
    created_at      timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT uk_resource_chunk UNIQUE (resource_id, chunk_idx),
    CONSTRAINT fk_chunk_resource FOREIGN KEY (resource_id) REFERENCES resource (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS resource_chunk_embedding_hnsw
    ON resource_chunk
    USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS resource_chunk_resource_id_idx ON resource_chunk (resource_id);
CREATE INDEX IF NOT EXISTS resource_chunk_embedding_model_idx ON resource_chunk (embedding_model);
CREATE INDEX IF NOT EXISTS resource_chunk_field_name_idx ON resource_chunk (field_name);
