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
            ALTER COLUMN values TYPE timestamp(6) with time zone
            USING values AT TIME ZONE 'UTC';
    END IF;
END $$;

-- Add actor audit columns to existing resource rows and backfill legacy values.
-- Historical rows predate actor auditing, so they are backfilled as 'legacy'.
-- Runtime writes without caller context continue to fall back to 'system' in service code.
DO $$
BEGIN
    IF to_regclass('public.resource') IS NOT NULL THEN
        ALTER TABLE public.resource
            ADD COLUMN IF NOT EXISTS created_by character varying(255);

        ALTER TABLE public.resource
            ADD COLUMN IF NOT EXISTS modified_by character varying(255);

        UPDATE public.resource
        SET created_by = COALESCE(created_by, 'legacy'),
            modified_by = COALESCE(modified_by, created_by, 'legacy');

        ALTER TABLE public.resource
            ALTER COLUMN created_by SET NOT NULL;

        ALTER TABLE public.resource
            ALTER COLUMN modified_by SET NOT NULL;
    END IF;
END $$;

-- Add actor audit columns to existing resource type rows and backfill legacy values.
DO $$
BEGIN
    IF to_regclass('public.resourcetype') IS NOT NULL THEN
        ALTER TABLE public.resourcetype
            ADD COLUMN IF NOT EXISTS created_by character varying(255);

        ALTER TABLE public.resourcetype
            ADD COLUMN IF NOT EXISTS modified_by character varying(255);

        UPDATE public.resourcetype
        SET created_by = COALESCE(created_by, 'legacy'),
            modified_by = COALESCE(modified_by, created_by, 'legacy');

        ALTER TABLE public.resourcetype
            ALTER COLUMN created_by SET NOT NULL;

        ALTER TABLE public.resourcetype
            ALTER COLUMN modified_by SET NOT NULL;
    END IF;
END $$;

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
