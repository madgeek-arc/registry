DO $$
BEGIN
    IF to_regclass('public.indexfield') IS NOT NULL THEN
        ALTER TABLE indexfield
            ADD COLUMN IF NOT EXISTS search_capabilities varchar(255);
    END IF;
END $$;
