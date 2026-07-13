--
-- Dedicated resourceType ("widget") for general TypedResourceController CRUD integration tests.
-- Uses a JSON payload (plain Jackson deserialization) rather than reusing the shared "employee"
-- fixture from data.sql, whose XML payload requires a JAXB-registered class unrelated to what
-- these tests are actually verifying.
--

INSERT INTO public.resourcetype (name, creation_date, created_by, indexmapperclass, modification_date, modified_by, payloadtype,
                                 schema, schemaurl)
VALUES ('widget', '2018-09-19 15:59:22.122', 'legacy', 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper',
        '2018-09-19 15:59:22.122', 'legacy', 'json', 'not_set', 'not_set')
ON CONFLICT (name) DO NOTHING;

-- GenericResourceManager.getClassFromResourceType() requires a "class" property to be present
-- (any value) to avoid a NullPointerException; java.util.LinkedHashMap is resolvable and lets
-- Jackson deserialize the JSON payload generically.
INSERT INTO public.resourcetype_properties (resourcetype_name, properties, properties_key)
VALUES ('widget', 'java.util.LinkedHashMap', 'class')
ON CONFLICT (resourcetype_name, properties_key) DO NOTHING;

INSERT INTO public.indexfield
(name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES
('code', NULL, 'code', false, '$.code', true, 'java.lang.String', 'widget'),
('label', NULL, 'label', false, '$.label', false, 'java.lang.String', 'widget')
ON CONFLICT (name, resourcetype_name) DO NOTHING;
