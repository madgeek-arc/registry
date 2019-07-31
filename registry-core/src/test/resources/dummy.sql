--
-- PostgreSQL database dump
--

-- Dumped from database version 10.5 (Debian 10.5-1.pgdg90+1)
-- Dumped by pg_dump version 10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)

--
-- Name: plpgsql; Type: EXTENSION; Schema: -; Owner: 
--


CREATE TABLE batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone,
    job_configuration_location character varying(2500)
);



--
-- Name: batch_job_execution_context; Type: TABLE; Schema: public; Owner: vrasidas
--

CREATE TABLE batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);



CREATE TABLE batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    type_cd character varying(6) NOT NULL,
    key_name character varying(100) NOT NULL,
    string_val text,
    date_val timestamp without time zone,
    long_val bigint,
    double_val double precision,
    identifying character(1) NOT NULL
);


--
-- Name: batch_job_execution_seq; Type: SEQUENCE; Schema: public; Owner: vrasidas
--

--
-- Name: batch_job_instance; Type: TABLE; Schema: public; Owner: vrasidas
--

CREATE TABLE batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);



CREATE TABLE batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    start_time timestamp without time zone NOT NULL,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);



CREATE TABLE batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);




CREATE TABLE booleanindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE booleanindexedfield_values (
    booleanindexedfield_id integer NOT NULL,
    "values" boolean
);



CREATE TABLE dateindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE dateindexedfield_values (
    dateindexedfield_id integer NOT NULL,
    "values" timestamp without time zone
);



CREATE TABLE floatindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE floatindexedfield_values (
    floatindexedfield_id integer NOT NULL,
    "values" double precision
);



CREATE TABLE flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);



CREATE TABLE indexfield (
    name character varying(255) NOT NULL,
    defaultvalue character varying(255),
    label character varying(255),
    multivalued boolean,
    path character varying(255),
    primarykey boolean,
    type character varying(255),
    resourcetype_name character varying(255) NOT NULL
);



CREATE TABLE integerindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE integerindexedfield_values (
    integerindexedfield_id integer NOT NULL,
    "values" bigint
);



CREATE TABLE longindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE longindexedfield_values (
    longindexedfield_id integer NOT NULL,
    "values" bigint
);




CREATE TABLE resource (
    id character varying(255) NOT NULL,
    creation_date timestamp without time zone NOT NULL,
    modification_date timestamp without time zone NOT NULL,
    payload text NOT NULL,
    payloadformat character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    fk_name character varying(255) NOT NULL
);



CREATE TABLE resourcetype (
    name character varying(255) NOT NULL,
    aliasgroup character varying(255),
    creation_date timestamp without time zone NOT NULL,
    indexmapperclass character varying(255),
    modification_date timestamp without time zone NOT NULL,
    payloadtype character varying(255) NOT NULL,
    schema text NOT NULL,
    schemaurl character varying(255)
);



CREATE TABLE resourcetype_indexfield (
    resourcetype_name character varying(255) NOT NULL,
    indexfields_resourcetype_name character varying(255) NOT NULL,
    indexfields_name character varying(255) NOT NULL
);


CREATE TABLE resourceversion (
    id character varying(255) NOT NULL,
    creation_date timestamp without time zone NOT NULL,
    parent_id character varying(255),
    payload text NOT NULL,
    resourcetype_name character varying(255),
    version character varying(255) NOT NULL,
    reference_id character varying(255),
    fk_name_version character varying(255)
);

CREATE TABLE schemadatabase (
    id character varying(255) NOT NULL,
    originalurl character varying(1000),
    schema text NOT NULL
);

CREATE TABLE stringindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE stringindexedfield_values (
    stringindexedfield_id integer NOT NULL,
    "values" text
);


ALTER TABLE ONLY batch_job_execution_context
    ADD CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_execution batch_job_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_instance batch_job_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_instance
    ADD CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id);


--
-- Name: batch_step_execution_context batch_step_execution_context_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);


--
-- Name: batch_step_execution batch_step_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_step_execution
    ADD CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id);


--
-- Name: booleanindexedfield booleanindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY booleanindexedfield
    ADD CONSTRAINT booleanindexedfield_pkey PRIMARY KEY (id);


--
-- Name: dateindexedfield dateindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY dateindexedfield
    ADD CONSTRAINT dateindexedfield_pkey PRIMARY KEY (id);


--
-- Name: floatindexedfield floatindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY floatindexedfield
    ADD CONSTRAINT floatindexedfield_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: indexfield indexfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY indexfield
    ADD CONSTRAINT indexfield_pkey PRIMARY KEY (resourcetype_name, name);


--
-- Name: integerindexedfield integerindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY integerindexedfield
    ADD CONSTRAINT integerindexedfield_pkey PRIMARY KEY (id);


--
-- Name: batch_job_instance job_inst_un; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_instance
    ADD CONSTRAINT job_inst_un UNIQUE (job_name, job_key);


--
-- Name: longindexedfield longindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY longindexedfield
    ADD CONSTRAINT longindexedfield_pkey PRIMARY KEY (id);


--
-- Name: resource resource_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resource
    ADD CONSTRAINT resource_pkey PRIMARY KEY (id);


--
-- Name: resourcetype resourcetype_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourcetype
    ADD CONSTRAINT resourcetype_pkey PRIMARY KEY (name);


--
-- Name: resourceversion resourceversion_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourceversion
    ADD CONSTRAINT resourceversion_pkey PRIMARY KEY (id);


--
-- Name: schemadatabase schemadatabase_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY schemadatabase
    ADD CONSTRAINT schemadatabase_pkey PRIMARY KEY (id);


--
-- Name: stringindexedfield stringindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY stringindexedfield
    ADD CONSTRAINT stringindexedfield_pkey PRIMARY KEY (id);


--
-- Name: resourcetype_indexfield uk_iq7il1d8r9su5u0d7jcq3p4ug; Type: CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourcetype_indexfield
    ADD CONSTRAINT uk_iq7il1d8r9su5u0d7jcq3p4ug UNIQUE (indexfields_resourcetype_name, indexfields_name);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: vrasidas
--

CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history USING btree (success);


--
-- Name: floatindexedfield_values fk6v8gu63ovpqbxsk10asfg8oq2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY floatindexedfield_values
    ADD CONSTRAINT fk6v8gu63ovpqbxsk10asfg8oq2 FOREIGN KEY (floatindexedfield_id) REFERENCES floatindexedfield(id);


--
-- Name: resourcetype_indexfield fk7sik7aiiq0v42m8coyppdowsg; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourcetype_indexfield
    ADD CONSTRAINT fk7sik7aiiq0v42m8coyppdowsg FOREIGN KEY (resourcetype_name) REFERENCES resourcetype(name);


--
-- Name: resource fk90kvqr4idlat5pee5tamehfpu; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resource
    ADD CONSTRAINT fk90kvqr4idlat5pee5tamehfpu FOREIGN KEY (fk_name) REFERENCES resourcetype(name);


--
-- Name: dateindexedfield_values fk9a3s750lj9iu60jbm2twwt9p6; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY dateindexedfield_values
    ADD CONSTRAINT fk9a3s750lj9iu60jbm2twwt9p6 FOREIGN KEY (dateindexedfield_id) REFERENCES dateindexedfield(id);


--
-- Name: booleanindexedfield fk_1fkd81c5w5hes5rw5y47d9rb2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY booleanindexedfield
    ADD CONSTRAINT fk_1fkd81c5w5hes5rw5y47d9rb2 FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: floatindexedfield fk_6c41agke54vxn511raoxrprf1; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY floatindexedfield
    ADD CONSTRAINT fk_6c41agke54vxn511raoxrprf1 FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: integerindexedfield fk_7wo5ce26ppipju2al2yj2mgrh; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY integerindexedfield
    ADD CONSTRAINT fk_7wo5ce26ppipju2al2yj2mgrh FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: longindexedfield fk_g768fvk33224p605911uce1vm; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY longindexedfield
    ADD CONSTRAINT fk_g768fvk33224p605911uce1vm FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: dateindexedfield fk_hcnmu0kxpthc8fy42vrdnx1gd; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY dateindexedfield
    ADD CONSTRAINT fk_hcnmu0kxpthc8fy42vrdnx1gd FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: stringindexedfield fk_pqi7pbaye7s6mffq25irom10q; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY stringindexedfield
    ADD CONSTRAINT fk_pqi7pbaye7s6mffq25irom10q FOREIGN KEY (resource_id) REFERENCES resource(id);


--
-- Name: resourcetype_indexfield fkd89saslxgqkjk9xe5le8xya66; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourcetype_indexfield
    ADD CONSTRAINT fkd89saslxgqkjk9xe5le8xya66 FOREIGN KEY (indexfields_resourcetype_name, indexfields_name) REFERENCES indexfield(resourcetype_name, name);


--
-- Name: stringindexedfield_values fkekj0s114ihg96iftnijf668x2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY stringindexedfield_values
    ADD CONSTRAINT fkekj0s114ihg96iftnijf668x2 FOREIGN KEY (stringindexedfield_id) REFERENCES stringindexedfield(id);


--
-- Name: indexfield fkhv3nafd941tssde6y2vh8cwx0; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY indexfield
    ADD CONSTRAINT fkhv3nafd941tssde6y2vh8cwx0 FOREIGN KEY (resourcetype_name) REFERENCES resourcetype(name);


--
-- Name: resourceversion fkn0tq8akvjeyjk7v9obhu56s6m; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourceversion
    ADD CONSTRAINT fkn0tq8akvjeyjk7v9obhu56s6m FOREIGN KEY (fk_name_version) REFERENCES resourcetype(name);


--
-- Name: longindexedfield_values fko3pakurnjvq9r24qffry92hcc; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY longindexedfield_values
    ADD CONSTRAINT fko3pakurnjvq9r24qffry92hcc FOREIGN KEY (longindexedfield_id) REFERENCES longindexedfield(id);


--
-- Name: booleanindexedfield_values fkol4dou8gqgg0ik8yd85usenf5; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY booleanindexedfield_values
    ADD CONSTRAINT fkol4dou8gqgg0ik8yd85usenf5 FOREIGN KEY (booleanindexedfield_id) REFERENCES booleanindexedfield(id);


--
-- Name: resourceversion fkrq94kmfewfixdnhss7vhp5d2f; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY resourceversion
    ADD CONSTRAINT fkrq94kmfewfixdnhss7vhp5d2f FOREIGN KEY (reference_id) REFERENCES resource(id);


--
-- Name: integerindexedfield_values fksuxndi576ylkdfs6h6afr6ckx; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY integerindexedfield_values
    ADD CONSTRAINT fksuxndi576ylkdfs6h6afr6ckx FOREIGN KEY (integerindexedfield_id) REFERENCES integerindexedfield(id);


--
-- Name: batch_job_execution_context job_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_execution_context
    ADD CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution_params job_exec_params_fk; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id);


--
-- Name: batch_step_execution job_exec_step_fk; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_step_execution
    ADD CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution job_inst_exec_fk; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES batch_job_instance(job_instance_id);


--
-- Name: batch_step_execution_context step_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
--

ALTER TABLE ONLY batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES batch_step_execution(step_execution_id);

--
-- PostgreSQL database dump complete
--

