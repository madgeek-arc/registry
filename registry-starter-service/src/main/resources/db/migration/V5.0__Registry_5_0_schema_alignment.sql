CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS tablefunc;

-- Core registry tables.
CREATE TABLE IF NOT EXISTS public.resourcetype (
    name varchar(50) PRIMARY KEY,
    schema text NOT NULL,
    schemaurl varchar(255),
    payloadtype varchar(30) NOT NULL,
    creation_date timestamptz NOT NULL,
    modification_date timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    modified_by varchar(255) NOT NULL,
    indexmapperclass varchar(255)
);

CREATE TABLE IF NOT EXISTS public.resource (
    id varchar(255) PRIMARY KEY,
    fk_name varchar(255) NOT NULL REFERENCES public.resourcetype(name),
    version varchar(50) NOT NULL,
    payload text NOT NULL,
    payloadformat varchar(30) NOT NULL,
    creation_date timestamptz NOT NULL,
    modification_date timestamptz NOT NULL,
    created_by varchar(255) NOT NULL,
    modified_by varchar(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS public.indexfield (
    resourcetype_name varchar(255) NOT NULL REFERENCES public.resourcetype(name),
    name varchar(255) NOT NULL,
    path varchar(255),
    type varchar(255),
    label varchar(255),
    defaultvalue varchar(255),
    multivalued boolean,
    primarykey boolean,
    search_capabilities varchar(255),
    embedding_weight real DEFAULT 0.0 CHECK (embedding_weight >= 0),
    related_resource_type varchar(255),
    related_resource_type_field varchar(255),
    PRIMARY KEY (name, resourcetype_name)
);

CREATE TABLE IF NOT EXISTS public.resourcetype_aliases (
    resourcetype_name varchar(255) NOT NULL REFERENCES public.resourcetype(name),
    aliases varchar(255)
);

CREATE TABLE IF NOT EXISTS public.resourcetype_properties (
    resourcetype_name varchar(255) NOT NULL REFERENCES public.resourcetype(name),
    properties varchar(255),
    properties_key varchar(255) NOT NULL,
    PRIMARY KEY (resourcetype_name, properties_key)
);

CREATE TABLE IF NOT EXISTS public.resourceversion (
    id varchar(255) PRIMARY KEY,
    reference_id varchar(255) REFERENCES public.resource(id),
    parent_id varchar(255),
    resourcetype_name varchar(255),
    fk_name_version varchar(255) REFERENCES public.resourcetype(name),
    version varchar(50) NOT NULL,
    payload text NOT NULL,
    creation_date timestamptz NOT NULL
);

CREATE TABLE IF NOT EXISTS public.schemadatabase (
    id varchar(400) PRIMARY KEY,
    originalurl varchar(1000),
    schema text NOT NULL
);

CREATE TABLE IF NOT EXISTS public.booleanindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.booleanindexedfield_values (
    booleanindexedfield_id integer NOT NULL REFERENCES public.booleanindexedfield(id),
    "values" boolean
);

CREATE TABLE IF NOT EXISTS public.dateindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.dateindexedfield_values (
    dateindexedfield_id integer NOT NULL REFERENCES public.dateindexedfield(id),
    "values" timestamptz
);

CREATE TABLE IF NOT EXISTS public.floatindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.floatindexedfield_values (
    floatindexedfield_id integer NOT NULL REFERENCES public.floatindexedfield(id),
    "values" double precision
);

CREATE TABLE IF NOT EXISTS public.integerindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.integerindexedfield_values (
    integerindexedfield_id integer NOT NULL REFERENCES public.integerindexedfield(id),
    "values" bigint
);

CREATE TABLE IF NOT EXISTS public.longindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.longindexedfield_values (
    longindexedfield_id integer NOT NULL REFERENCES public.longindexedfield(id),
    "values" bigint
);

CREATE TABLE IF NOT EXISTS public.stringindexedfield (
    id integer PRIMARY KEY,
    name varchar(255),
    resource_id varchar(255) REFERENCES public.resource(id)
);

CREATE TABLE IF NOT EXISTS public.stringindexedfield_values (
    stringindexedfield_id integer NOT NULL REFERENCES public.stringindexedfield(id),
    "values" text
);

-- Bring pre-existing registry tables up to the current schema.
ALTER TABLE public.resourcetype
    ADD COLUMN IF NOT EXISTS created_by varchar(255),
    ADD COLUMN IF NOT EXISTS modified_by varchar(255),
    ADD COLUMN IF NOT EXISTS indexmapperclass varchar(255),
    ADD COLUMN IF NOT EXISTS schemaurl varchar(255);

ALTER TABLE public.resource
    ADD COLUMN IF NOT EXISTS created_by varchar(255),
    ADD COLUMN IF NOT EXISTS modified_by varchar(255);

ALTER TABLE public.indexfield
    ADD COLUMN IF NOT EXISTS embedding_weight real DEFAULT 0.0,
    ADD COLUMN IF NOT EXISTS search_capabilities varchar(255),
    ADD COLUMN IF NOT EXISTS related_resource_type varchar(255),
    ADD COLUMN IF NOT EXISTS related_resource_type_field varchar(255);

COMMENT ON COLUMN public.indexfield.search_capabilities IS
    'Search capabilities for string fields. Defaults to KEYWORD when unset.';
COMMENT ON COLUMN public.indexfield.embedding_weight IS
    'The weight this index field will have when creating an embedding vector for the resource.';
COMMENT ON COLUMN public.indexfield.related_resource_type IS
    'The name of the ResourceType whose resource IDs appear as values for this field. When set, FacetLabelService will resolve Value.label for facets backed by this field.';
COMMENT ON COLUMN public.indexfield.related_resource_type_field IS
    'The IndexField name in the relatedResourceType to use as the display label. Falls back to a field named name in the related type if null.';

-- Existing resource-type views depend on timestamp columns that are normalized below.
-- PostgreSQL cannot alter a column type while a view references it, so drop the
-- generated views first and recreate them from current indexfield metadata later.
CREATE TEMP TABLE registry_v5_resource_views_to_recreate ON COMMIT DROP AS
SELECT rt.name
FROM public.resourcetype rt
JOIN information_schema.views v
  ON v.table_schema = 'public'
 AND v.table_name = rt.name || '_view';

DO $$
DECLARE
    view_name text;
BEGIN
    FOR view_name IN
        SELECT name || '_view'
        FROM registry_v5_resource_views_to_recreate
    LOOP
        EXECUTE format('DROP VIEW IF EXISTS public.%I', view_name);
    END LOOP;
END $$;

-- Normalize legacy naive audit/version timestamps to timestamptz using UTC.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'resource'
          AND column_name = 'creation_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.resource
            ALTER COLUMN creation_date TYPE timestamptz
            USING creation_date AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'resource'
          AND column_name = 'modification_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.resource
            ALTER COLUMN modification_date TYPE timestamptz
            USING modification_date AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'resourcetype'
          AND column_name = 'creation_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.resourcetype
            ALTER COLUMN creation_date TYPE timestamptz
            USING creation_date AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'resourcetype'
          AND column_name = 'modification_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.resourcetype
            ALTER COLUMN modification_date TYPE timestamptz
            USING modification_date AT TIME ZONE 'UTC';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'resourceversion'
          AND column_name = 'creation_date'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.resourceversion
            ALTER COLUMN creation_date TYPE timestamptz
            USING creation_date AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Normalize legacy naive timestamps to timestamptz for dateindexedfield values.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'dateindexedfield_values'
          AND column_name = 'values'
          AND data_type = 'timestamp without time zone'
    ) THEN
        ALTER TABLE public.dateindexedfield_values
            ALTER COLUMN values TYPE timestamptz
            USING values AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Recreate generated resource-type views after timestamp column types are stable.
DO $$
DECLARE
    resource_type record;
    field_group record;
    view_sql text;
    source_sql text;
    output_columns text;
BEGIN
    FOR resource_type IN
        SELECT name
        FROM registry_v5_resource_views_to_recreate
        ORDER BY name
    LOOP
        view_sql := format(
            'CREATE VIEW public.%I AS (SELECT * FROM (select id, creation_date, modification_date from resource where fk_name=%L) r',
            resource_type.name || '_view',
            resource_type.name
        );

        FOR field_group IN
            SELECT field_table,
                   value_type,
                   multivalued,
                   string_agg(quote_literal(name), ', ' ORDER BY lower(name)) AS field_names,
                   min(lower(name)) AS first_field_name
            FROM (
                SELECT name,
                       COALESCE(multivalued, false) AS multivalued,
                       CASE type
                           WHEN 'java.lang.Float' THEN 'floatindexedfield'
                           WHEN 'java.lang.Integer' THEN 'integerindexedfield'
                           WHEN 'java.lang.String' THEN 'stringindexedfield'
                           WHEN 'java.lang.Boolean' THEN 'booleanindexedfield'
                           WHEN 'java.lang.Long' THEN 'longindexedfield'
                           WHEN 'java.util.Date' THEN 'dateindexedfield'
                           WHEN 'java.time.Instant' THEN 'dateindexedfield'
                       END AS field_table,
                       CASE type
                           WHEN 'java.lang.Float' THEN 'float'
                           WHEN 'java.lang.Integer' THEN 'bigint'
                           WHEN 'java.lang.String' THEN 'text'
                           WHEN 'java.lang.Boolean' THEN 'bool'
                           WHEN 'java.lang.Long' THEN 'bigint'
                           WHEN 'java.util.Date' THEN 'timestamp with time zone'
                           WHEN 'java.time.Instant' THEN 'timestamp with time zone'
                       END AS value_type
                FROM public.indexfield
                WHERE resourcetype_name = resource_type.name
            ) typed_fields
            WHERE field_table IS NOT NULL
            GROUP BY field_table, value_type, multivalued
            ORDER BY multivalued, first_field_name
        LOOP
            SELECT string_agg(format('%s %s%s', name, field_group.value_type,
                                     CASE WHEN field_group.multivalued THEN '[]' ELSE '' END), ', ' ORDER BY lower(name))
            INTO output_columns
            FROM public.indexfield
            WHERE resourcetype_name = resource_type.name
              AND COALESCE(multivalued, false) IS NOT DISTINCT FROM field_group.multivalued
              AND CASE type
                      WHEN 'java.lang.Float' THEN 'floatindexedfield'
                      WHEN 'java.lang.Integer' THEN 'integerindexedfield'
                      WHEN 'java.lang.String' THEN 'stringindexedfield'
                      WHEN 'java.lang.Boolean' THEN 'booleanindexedfield'
                      WHEN 'java.lang.Long' THEN 'longindexedfield'
                      WHEN 'java.util.Date' THEN 'dateindexedfield'
                      WHEN 'java.time.Instant' THEN 'dateindexedfield'
                  END = field_group.field_table;

            IF field_group.multivalued THEN
                source_sql := format(
                    'SELECT i.resource_id, i.name, array_remove(array_agg(v.values), NULL) FROM (' ||
                    'SELECT %1$I.id, %1$I.name, %1$I.resource_id FROM resource r, %1$I ' ||
                    'WHERE r.fk_name = %2$L AND r.id = %1$I.resource_id AND %1$I.name IN(%3$s) ORDER BY %1$I.name) i ' ||
                    'LEFT JOIN (SELECT %1$I.id, %1$I_values.values FROM %1$I, %1$I_values ' ||
                    'WHERE %1$I.id = %1$I_values.%1$I_id) v ON i.id = v.id ' ||
                    'GROUP BY i.resource_id, i.name ORDER BY i.resource_id, i.name',
                    field_group.field_table,
                    resource_type.name,
                    field_group.field_names
                );
                view_sql := view_sql || format(
                    ' INNER JOIN (SELECT * FROM crosstab(%L) AS output_tbl(id varchar(255), %s)) m_%I USING(id)',
                    source_sql,
                    output_columns,
                    field_group.field_table
                );
            ELSE
                source_sql := format(
                    'SELECT i.resource_id, i.name, v.values FROM (' ||
                    'SELECT %1$I.id, %1$I.name, %1$I.resource_id FROM resource r, %1$I ' ||
                    'WHERE r.fk_name = %2$L AND r.id = %1$I.resource_id AND %1$I.name IN(%3$s) ORDER BY %1$I.name) i ' ||
                    'LEFT JOIN (SELECT %1$I.id, %1$I_values.values FROM %1$I, %1$I_values ' ||
                    'WHERE %1$I.id = %1$I_values.%1$I_id) v ON i.id = v.id ' ||
                    'ORDER BY i.resource_id, i.name',
                    field_group.field_table,
                    resource_type.name,
                    field_group.field_names
                );
                view_sql := view_sql || format(
                    ' INNER JOIN (SELECT * FROM crosstab(%L) AS output_tbl(id varchar(255), %s)) s_%I USING(id)',
                    source_sql,
                    output_columns,
                    field_group.field_table
                );
            END IF;
        END LOOP;

        view_sql := view_sql || ')';
        EXECUTE view_sql;
    END LOOP;
END $$;

-- Add actor audit columns to existing resource rows and backfill legacy values.
-- Historical rows predate actor auditing, so they are backfilled as 'legacy'.
-- Runtime writes without caller context continue to fall back to 'system' in service code.
UPDATE public.resource
SET created_by = COALESCE(created_by, 'legacy'),
    modified_by = COALESCE(modified_by, created_by, 'legacy');

-- Add actor audit columns to existing resource type rows and backfill legacy values.
UPDATE public.resourcetype
SET created_by = COALESCE(created_by, 'legacy'),
    modified_by = COALESCE(modified_by, created_by, 'legacy');

ALTER TABLE public.resource
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

ALTER TABLE public.resourcetype
    ALTER COLUMN created_by SET NOT NULL,
    ALTER COLUMN modified_by SET NOT NULL;

-- Backfill embedding weights only for string fields; non-string fields are not embedded.
UPDATE public.indexfield
SET embedding_weight = 1.0
WHERE type = 'java.lang.String'
  AND embedding_weight IS NULL;

-- Add the search capabilities column and backfill string fields as KEYWORD,TEXT.
UPDATE public.indexfield
SET search_capabilities = 'KEYWORD,TEXT'
WHERE type = 'java.lang.String'
  AND (search_capabilities IS NULL OR btrim(search_capabilities) = '');

-- Remove the obsolete ResourceType-to-IndexField join table after validating FK-backed rows exist.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'resourcetype_indexfield'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM public.resourcetype_indexfield legacy
            LEFT JOIN public.indexfield idx
                ON idx.name = legacy.indexfields_name
               AND idx.resourcetype_name = legacy.indexfields_resourcetype_name
            WHERE idx.name IS NULL
               OR legacy.resourcetype_name IS DISTINCT FROM legacy.indexfields_resourcetype_name
        ) THEN
            RAISE EXCEPTION
                'Legacy resourcetype_indexfield rows are inconsistent with indexfield. Manual migration is required before upgrading.';
        END IF;

        DROP TABLE public.resourcetype_indexfield;
    END IF;
END $$;

-- PostgreSQL semantic search support.
CREATE TABLE IF NOT EXISTS public.resource_chunk (
    id bigserial PRIMARY KEY,
    resource_id text NOT NULL REFERENCES public.resource(id) ON DELETE CASCADE,
    chunk_idx integer NOT NULL,
    field_name varchar(255) NOT NULL,
    value_ordinal integer NOT NULL DEFAULT 0,
    field_chunk_idx integer NOT NULL DEFAULT 0,
    content text NOT NULL,
    embedding vector(384) NOT NULL,
    embedding_model varchar(255) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uk_resource_chunk UNIQUE (resource_id, chunk_idx)
);

CREATE INDEX IF NOT EXISTS resource_chunk_embedding_hnsw
    ON public.resource_chunk USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS resource_chunk_resource_id_idx
    ON public.resource_chunk (resource_id);
CREATE INDEX IF NOT EXISTS resource_chunk_embedding_model_idx
    ON public.resource_chunk (embedding_model);
CREATE INDEX IF NOT EXISTS resource_chunk_field_name_idx
    ON public.resource_chunk (field_name);
