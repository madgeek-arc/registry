--
-- Dedicated resourceType ("employee_slash") whose primary key (first_name) contains a literal
-- "/", reproducing consumers' "x/y" domain primary key shape for encoded-slash firewall tests.
-- Uses a JSON payload (plain Jackson deserialization) rather than reusing the shared "employee"
-- fixture from data.sql, whose XML payload requires a JAXB-registered class unrelated to what
-- this test is actually verifying.
--

INSERT INTO public.resourcetype (name, creation_date, created_by, indexmapperclass, modification_date, modified_by, payloadtype,
                                 schema, schemaurl)
VALUES ('employee_slash', '2018-09-19 15:59:22.122', 'legacy', 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper',
        '2018-09-19 15:59:22.122', 'legacy', 'json', 'not_set', 'not_set')
ON CONFLICT (name) DO NOTHING;

-- GenericResourceManager.getClassFromResourceType() requires a "class" property to be present
-- (any value) to avoid a NullPointerException; java.util.LinkedHashMap is resolvable and lets
-- Jackson deserialize the JSON payload generically.
INSERT INTO public.resourcetype_properties (resourcetype_name, properties, properties_key)
VALUES ('employee_slash', 'java.util.LinkedHashMap', 'class')
ON CONFLICT (resourcetype_name, properties_key) DO NOTHING;

INSERT INTO public.indexfield
(name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES
('first_name', NULL, 'first_name', false, '$.first_name', true, 'java.lang.String', 'employee_slash')
ON CONFLICT (name, resourcetype_name) DO NOTHING;

INSERT INTO public.resource (id, creation_date, modification_date, created_by, modified_by, payload, payloadformat, version, fk_name)
VALUES ('a1b2c3d4-e5f6-4789-a123-456789abcdef', '2018-09-19 15:59:22.122', '2018-09-19 15:59:22.122', 'legacy', 'legacy',
        '{"first_name":"idPrefix/idSuffix"}', 'json', '12032018130400', 'employee_slash')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.stringindexedfield (id, name, resource_id)
VALUES (223128, 'first_name', 'a1b2c3d4-e5f6-4789-a123-456789abcdef')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
SELECT 223128, 'idPrefix/idSuffix'
WHERE NOT EXISTS (
    SELECT 1 FROM public.stringindexedfield_values WHERE stringindexedfield_id = 223128
);
