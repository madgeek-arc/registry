DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_class WHERE relname = 'hibernate_sequence' AND relkind = 'S') THEN
        ALTER SEQUENCE hibernate_sequence RENAME TO indexedfield_seq;
        ALTER SEQUENCE indexedfield_seq INCREMENT BY 50;
    END IF;
END $$;