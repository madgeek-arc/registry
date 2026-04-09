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
