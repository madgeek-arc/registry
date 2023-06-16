--
-- PostgreSQL database dump
--


--
-- Data for Name: resourcetype; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.resourcetype (name, aliasgroup, creation_date, indexmapperclass, modification_date, payloadtype, schema, schemaurl)
VALUES ('employee', 'resourceTypes', '2018-12-03 13:03:59.871', 'eu.openminted.registry.core.index.DefaultIndexMapper', '2018-12-03 13:03:59.871', 'xml', '

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
</xs:schema>', 'not_set');


--
-- Data for Name: resource; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.resource (id, creation_date, modification_date, payload, payloadformat, version, fk_name)
VALUES ('e98db949-f3e3-4d30-9894-7dd2e291fbef', '2018-09-19 15:59:22.122', '2018-09-19 15:59:22.122', '<?xml version="1.0"?> <employee> <author>Jodeee</author> <age>28</age> <single>false</single> <birthday>645544821000</birthday> <salary>1292.123</salary> <amka>051417010293821</amka></employee>
', 'xml', '12032018130400', 'employee');


--
-- Data for Name: booleanindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.booleanindexedfield (id, name, resource_id)
VALUES (123123, 'single', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: booleanindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.booleanindexedfield_values (booleanindexedfield_id, "values")
VALUES (123123, false);


--
-- Data for Name: dateindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.dateindexedfield (id, name, resource_id)
VALUES (123123, 'birthday', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: dateindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.dateindexedfield_values (dateindexedfield_id, "values")
VALUES (123123, '1990-06-16 17:00:21');


--
-- Data for Name: floatindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.floatindexedfield (id, name, resource_id)
VALUES (123123, 'salary', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: floatindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.floatindexedfield_values (floatindexedfield_id, "values")
VALUES (123123, 1292.12300000000005);



--
-- Data for Name: indexfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('first_name', NULL, 'first_name', false, '//*[local-name()=''author'']/text()', true, 'java.lang.String', 'employee');
INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('age', NULL, 'age', false, '//*[local-name()=''age'']/text()', false, 'java.lang.Integer', 'employee');
INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('single', NULL, 'single', false, '//*[local-name()=''single'']/text()', false, 'java.lang.Boolean', 'employee');
INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('birthday', NULL, 'birthday', false, '//*[local-name()=''birthday'']/text()', false, 'java.util.Date', 'employee');
INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('salary', NULL, 'salary', false, '//*[local-name()=''salary'']/text()', false, 'java.lang.Float', 'employee');
INSERT INTO public.indexfield (name, defaultvalue, label, multivalued, path, primarykey, type, resourcetype_name)
VALUES ('amka', NULL, 'amka', false, '//*[local-name()=''amka'']/text()', false, 'java.lang.Long', 'employee');


--
-- Data for Name: integerindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.integerindexedfield (id, name, resource_id)
VALUES (123123, 'age', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: integerindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.integerindexedfield_values (integerindexedfield_id, "values")
VALUES (123123, 28);


--
-- Data for Name: longindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.longindexedfield (id, name, resource_id)
VALUES (123123, 'amka', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: longindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.longindexedfield_values (longindexedfield_id, "values")
VALUES (123123, 51417010293821);


--
-- Data for Name: resourcetype_indexfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'first_name');
INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'age');
INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'single');
INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'birthday');
INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'salary');
INSERT INTO public.resourcetype_indexfield (resourcetype_name, indexfields_resourcetype_name, indexfields_name)
VALUES ('employee', 'employee', 'amka');


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
</xs:schema>');


--
-- Data for Name: stringindexedfield; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.stringindexedfield (id, name, resource_id)
VALUES (123123,'first_name', 'e98db949-f3e3-4d30-9894-7dd2e291fbef');


--
-- Data for Name: stringindexedfield_values; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.stringindexedfield_values (stringindexedfield_id, "values")
VALUES (123123, 'Jodeee');


--
-- PostgreSQL database dump complete
--

