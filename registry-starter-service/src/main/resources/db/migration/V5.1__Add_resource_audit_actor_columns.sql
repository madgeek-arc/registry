-- Historical rows predate actor auditing, so they are backfilled as 'legacy'.
-- Runtime writes without caller context continue to fall back to 'system' in service code.
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
