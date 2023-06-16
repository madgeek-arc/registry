--
-- PostgreSQL database dump
--

--
-- Data for Name: batch_job_instance; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_job_instance (job_instance_id, version, job_name, job_key)
VALUES (1, 0, 'restore', '97411dfcb23d12da43dfa78b6da678a1');


--
-- Data for Name: batch_job_execution; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_job_execution (job_execution_id, version, job_instance_id, create_time, start_time, end_time, status, exit_code, exit_message, last_updated, job_configuration_location)
VALUES (1, 2, 1, '2018-12-03 13:03:59.737', '2018-12-03 13:03:59.756', '2018-12-03 13:04:00.628', 'COMPLETED', 'COMPLETED', '', '2018-12-03 13:04:00.628', NULL);


--
-- Data for Name: batch_job_execution_context; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_job_execution_context (job_execution_id, short_context, serialized_context)
VALUES (1, '{"resources":["[Ljava.io.File;",["/home/jod/Desktop/apache-tomcat-8.5.34/temp/decompress1743764190167320258/employee/e98db949-f3e3-4d30-9894-7dd2e291fbef.json"]],"resourceType":["eu.openminted.registry.core.domain.ResourceType",{"name":"employee","schema":"\r\n\r\n<xs:schema attributeFormDefault=\"unqualified\" elementFormDefault=\"qualified\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\r\n  <xs:element name=\"employee\">\r\n    <xs:complexType>\r\n      <xs:sequence>\r\n        <xs:element type=\"xs:string\" name=\"author\"/>\r\n        <xs:element type=\"xs:byte\" name=\"age\"/>\r\n        <xs:element type=\"xs:string\" name=\"single\"/>\r\n        <xs:element type=\"xs:string\" name=\"birthday\"/>\r\n        <xs:element type=\"xs:float\" name=\"salary\"/>\r\n        <xs:element type=\"xs:long\" name=\"amka\"/>\r\n      </xs:sequence>\r\n    </xs:complexType>\r\n  </xs:element>\r\n</xs:schema>","schemaUrl":"not_set","payloadType":"xml","creationDate":1543835039871,"modificationDate":1543835039871,"indexMapperClass":"eu.openminted.registry.core.index.DefaultIndexMapper","indexFields":["org.hibernate.collection.internal.PersistentBag",[["eu.openminted.registry.core.domain.index.IndexField",{"name":"first_name","path":"//*[local-name()=''author'']/text()","type":"java.lang.String","label":"first_name","defaultValue":null,"multivalued":false,"primaryKey":true}],["eu.openminted.registry.core.domain.index.IndexField",{"name":"age","path":"//*[local-name()=''age'']/text()","type":"java.lang.Integer","label":"age","defaultValue":null,"multivalued":false,"primaryKey":false}],["eu.openminted.registry.core.domain.index.IndexField",{"name":"single","path":"//*[local-name()=''single'']/text()","type":"java.lang.Boolean","label":"single","defaultValue":null,"multivalued":false,"primaryKey":false}],["eu.openminted.registry.core.domain.index.IndexField",{"name":"birthday","path":"//*[local-name()=''birthday'']/text()","type":"java.util.Date","label":"birthday","defaultValue":null,"multivalued":false,"primaryKey":false}],["eu.openminted.registry.core.domain.index.IndexField",{"name":"salary","path":"//*[local-name()=''salary'']/text()","type":"java.lang.Float","label":"salary","defaultValue":null,"multivalued":false,"primaryKey":false}],["eu.openminted.registry.core.domain.index.IndexField",{"name":"amka","path":"//*[local-name()=''amka'']/text()","type":"java.lang.Long","label":"amka","defaultValue":null,"multivalued":false,"primaryKey":false}]]],"aliasGroup":"resourceTypes"}]}', NULL);


--
-- Data for Name: batch_job_execution_params; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_job_execution_params (job_execution_id, type_cd, key_name, string_val, date_val, long_val, double_val, identifying)
VALUES (1, 'STRING', 'resourceType', 'employee', '1970-01-01 02:00:00', 0, 0, 'Y');
INSERT INTO public.batch_job_execution_params (job_execution_id, type_cd, key_name, string_val, date_val, long_val, double_val, identifying)
VALUES (1, 'STRING', 'resourceTypeDir', '/home/jod/Desktop/apache-tomcat-8.5.34/temp/decompress1743764190167320258/employee', '1970-01-01 02:00:00', 0, 0, 'Y');
INSERT INTO public.batch_job_execution_params (job_execution_id, type_cd, key_name, string_val, date_val, long_val, double_val, identifying)
VALUES (1, 'DATE', 'date', '', '2018-12-03 13:03:59.648', 0, 0, 'Y');


--
-- Data for Name: batch_step_execution; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_step_execution (step_execution_id, version, step_name, job_execution_id, start_time, end_time, status, commit_count, read_count, filter_count, write_count, read_skip_count, write_skip_count, process_skip_count, rollback_count, exit_code, exit_message, last_updated)
VALUES (1, 3, 'resourceTypeStep', 1, '2018-12-03 13:03:59.783', '2018-12-03 13:03:59.988', 'COMPLETED', 1, 0, 0, 0, 0, 0, 0, 0, 'COMPLETED', '', '2018-12-03 13:03:59.988');
INSERT INTO public.batch_step_execution (step_execution_id, version, step_name, job_execution_id, start_time, end_time, status, commit_count, read_count, filter_count, write_count, read_skip_count, write_skip_count, process_skip_count, rollback_count, exit_code, exit_message, last_updated)
VALUES (2, 7, 'resourcesChunkStep', 1, '2018-12-03 13:04:00.039', '2018-12-03 13:04:00.624', 'COMPLETED', 5, 1, 0, 1, 0, 0, 0, 0, 'COMPLETED', '', '2018-12-03 13:04:00.624');


--
-- Data for Name: batch_step_execution_context; Type: TABLE DATA; Schema: public; Owner: vrasidas
--

INSERT INTO public.batch_step_execution_context (step_execution_id, short_context, serialized_context)
VALUES (1, '{"batch.taskletType":"eu.openminted.registry.core.backup.restore.RestoreResourceTypeStep$$EnhancerBySpringCGLIB$$24f07c0f","batch.stepType":"org.springframework.batch.core.step.tasklet.TaskletStep"}', NULL);
INSERT INTO public.batch_step_execution_context (step_execution_id, short_context, serialized_context)
VALUES (2, '{"batch.taskletType":"org.springframework.batch.core.step.item.ChunkOrientedTasklet","batch.stepType":"org.springframework.batch.core.step.tasklet.TaskletStep"}', NULL);


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

