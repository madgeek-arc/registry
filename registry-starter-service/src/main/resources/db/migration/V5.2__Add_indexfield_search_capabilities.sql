-- Add the search capabilities column and backfill string fields as KEYWORD,TEXT.
DO $$
BEGIN
    IF to_regclass('public.indexfield') IS NOT NULL THEN
        ALTER TABLE indexfield
            ADD COLUMN IF NOT EXISTS search_capabilities varchar(255);

        UPDATE indexfield
        SET search_capabilities = 'KEYWORD,TEXT'
        WHERE type = 'java.lang.String';
    END IF;
END $$;
