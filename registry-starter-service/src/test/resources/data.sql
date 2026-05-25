--
-- PostgreSQL database dump
--


--
-- Data for Name: resourcetype; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.resourcetype (name, creation_date, created_by, indexmapperclass, modification_date, modified_by, payloadtype,
                                 schema, schemaurl)
VALUES ('employee', '2018-12-03 13:03:59.871', 'legacy', 'gr.uoa.di.madgik.registry.index.DefaultIndexMapper',
        '2018-12-03 13:03:59.871', 'legacy', 'xml', '

<xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="employee">
    <xs:complexType>
      <xs:sequence>
        <xs:element type="xs:string" name="author"/>
        <xs:element type="xs:byte" name="age"/>
        <xs:element type="xs:string" name="single"/>
        <xs:element type="xs:string" name="birthday"/>
        <xs:element type="xs:float" name="salary"/>
        <xs:element type="xs:long" name="amka"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>', 'not_set')
ON CONFLICT (name) DO NOTHING;

INSERT INTO public.resourcetype_aliases (resourcetype_name, aliases)
SELECT 'employee', 'resourceTypes'
WHERE NOT EXISTS (
    SELECT 1 FROM public.resourcetype_aliases WHERE resourcetype_name = 'employee' AND aliases = 'resourceTypes'
);


--
-- Data for Name: resource; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.resource (id, creation_date, modification_date, created_by, modified_by, payload, payloadformat, version, fk_name)
VALUES ('e98db949-f3e3-4d30-9894-7dd2e291fbef', '2018-09-19 15:59:22.122', '2018-09-19 15:59:22.122', 'legacy', 'legacy', '<?xml version="1.0"?> <employee> <author>Jodeee</author> <age>28</age> <single>false</single> <birthday>645544821000</birthday> <salary>1292.123</salary> <amka>051417010293821</amka></employee>
', 'xml', '12032018130400', 'employee')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: booleanindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.booleanindexedfield (id, name, resource_id)
VALUES (123123, 'single', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: booleanindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.booleanindexedfield_values (booleanindexedfield_id, "values")
SELECT 123123, false
WHERE NOT EXISTS (
    SELECT 1 FROM public.booleanindexedfield_values WHERE booleanindexedfield_id = 123123
);


--
-- Data for Name: dateindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.dateindexedfield (id, name, resource_id)
VALUES (123124, 'birthday', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: dateindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.dateindexedfield_values (dateindexedfield_id, "values")
SELECT 123124, '1990-06-16 17:00:21'
WHERE NOT EXISTS (
    SELECT 1 FROM public.dateindexedfield_values WHERE dateindexedfield_id = 123124
);


--
-- Data for Name: floatindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.floatindexedfield (id, name, resource_id)
VALUES (123125, 'salary', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: floatindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.floatindexedfield_values (floatindexedfield_id, "values")
SELECT 123125, 1292.12300000000005
WHERE NOT EXISTS (
    SELECT 1 FROM public.floatindexedfield_values WHERE floatindexedfield_id = 123125
);

--
-- Data for Name: integerindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.integerindexedfield (id, name, resource_id)
VALUES (123126, 'age', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: integerindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.integerindexedfield_values (integerindexedfield_id, "values")
SELECT 123126, 28
WHERE NOT EXISTS (
    SELECT 1 FROM public.integerindexedfield_values WHERE integerindexedfield_id = 123126
);


--
-- Data for Name: longindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.longindexedfield (id, name, resource_id)
VALUES (123127, 'amka', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: longindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.longindexedfield_values (longindexedfield_id, "values")
SELECT 123127, 51417010293821
WHERE NOT EXISTS (
    SELECT 1 FROM public.longindexedfield_values WHERE longindexedfield_id = 123127
);


--
-- Data for Name: stringindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.stringindexedfield (id, name, resource_id)
VALUES (123128, 'first_name', 'e98db949-f3e3-4d30-9894-7dd2e291fbef')
ON CONFLICT (id) DO NOTHING;


--
-- Data for Name: stringindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
SELECT 123128, 'Jodeee'
WHERE NOT EXISTS (
    SELECT 1 FROM public.stringindexedfield_values WHERE stringindexedfield_id = 123128
);

--
-- Data for Name: indexfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.indexfield
(name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES
('first_name', NULL, 'first_name', false, '//*[local-name()=''author'']/text()', true, 'java.lang.String', 'employee'),
('age', NULL, 'age', false, '//*[local-name()=''age'']/text()', false, 'java.lang.Integer', 'employee'),
('single', NULL, 'single', false, '//*[local-name()=''single'']/text()', false, 'java.lang.Boolean', 'employee'),
('birthday', NULL, 'birthday', false, '//*[local-name()=''birthday'']/text()', false, 'java.time.Instant','employee'),
('salary', NULL, 'salary', false, '//*[local-name()=''salary'']/text()', false, 'java.lang.Float', 'employee'),
('amka', NULL, 'amka', false, '//*[local-name()=''amka'']/text()', false, 'java.lang.Long', 'employee')
ON CONFLICT (name, resourcetype_name) DO NOTHING;


--
-- Data for Name: resourceversion; Type: TABLE DATA; Schema: public; Owner: vrasidas
--


--
-- Data for Name: schemadatabase; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.schemadatabase (id, originalurl, schema)
VALUES ('cccbd2ae2abfd0bb0d1c6c2216116ed1', 'employee', '

<xs:schema attributeFormDefault="unqualified" elementFormDefault="qualified" xmlns:xs="http://www.w3.org/2001/XMLSchema">
  <xs:element name="employee">
    <xs:complexType>
      <xs:sequence>
        <xs:element type="xs:string" name="author"/>
        <xs:element type="xs:byte" name="age"/>
        <xs:element type="xs:string" name="single"/>
        <xs:element type="xs:string" name="birthday"/>
        <xs:element type="xs:float" name="salary"/>
        <xs:element type="xs:long" name="amka"/>
      </xs:sequence>
    </xs:complexType>
  </xs:element>
</xs:schema>')
ON CONFLICT (id) DO NOTHING;


--
-- PostgreSQL database dump complete
--
