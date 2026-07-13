--
-- Dedicated resourceType ("gadget") with a composite primary key (vendor + sku) for
-- TypedResourceController /key route integration tests. All current fixtures used elsewhere have
-- exactly one primarykey=true field, so composite-key behavior needs its own resourceType.
--

INSERT INTO public.resourcetype (name, creation_date, created_by, indexmapperclass, modification_date, modified_by, payloadtype,
                                 schema, schemaurl)
VALUES ('gadget', '2018-09-19 15:59:22.122', 'legacy', 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper',
        '2018-09-19 15:59:22.122', 'legacy', 'json', 'not_set', 'not_set')
ON CONFLICT (name) DO NOTHING;

-- GenericResourceManager.getClassFromResourceType() requires a "class" property to be present
-- (any value) to avoid a NullPointerException; java.util.LinkedHashMap is resolvable and lets
-- Jackson deserialize the JSON payload generically.
INSERT INTO public.resourcetype_properties (resourcetype_name, properties, properties_key)
VALUES ('gadget', 'java.util.LinkedHashMap', 'class')
ON CONFLICT (resourcetype_name, properties_key) DO NOTHING;

INSERT INTO public.indexfield
(name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES
('vendor', NULL, 'vendor', false, '$.vendor', true, 'java.lang.String', 'gadget'),
('sku', NULL, 'sku', false, '$.sku', true, 'java.lang.String', 'gadget'),
('label', NULL, 'label', false, '$.label', false, 'java.lang.String', 'gadget')
ON CONFLICT (name, resourcetype_name) DO NOTHING;
