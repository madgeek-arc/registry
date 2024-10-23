--
-- PostgreSQL database dump
--

-- Dumped from database version 10.5 (Debian 10.5-1.pgdg90+1)
-- Dumped by pg_dump version 10.6 (Ubuntu 10.6-0ubuntu0.18.04.1)


CREATE SEQUENCE IF NOT EXISTS hibernate_sequence START WITH 1;


CREATE TABLE IF NOT EXISTS booleanindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE IF NOT EXISTS booleanindexedfield_values (
    booleanindexedfield_id integer NOT NULL,
    "values" boolean
);



CREATE TABLE IF NOT EXISTS dateindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE IF NOT EXISTS dateindexedfield_values (
    dateindexedfield_id integer NOT NULL,
    "values" timestamp without time zone
);



CREATE TABLE IF NOT EXISTS floatindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);



CREATE TABLE IF NOT EXISTS floatindexedfield_values (
    floatindexedfield_id integer NOT NULL,
    "values" double precision
);



CREATE TABLE IF NOT EXISTS flyway_schema_history (
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



CREATE TABLE IF NOT EXISTS indexfield (
    name character varying(255) NOT NULL,
    defaultvalue character varying(255),
    label character varying(255),
    multivalued boolean,
    path character varying(255),
    primarykey boolean,
    type character varying(255),
    resourcetype_name character varying(255) NOT NULL
);



CREATE TABLE IF NOT EXISTS integerindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE IF NOT EXISTS integerindexedfield_values (
    integerindexedfield_id integer NOT NULL,
    "values" bigint
);



CREATE TABLE IF NOT EXISTS longindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE IF NOT EXISTS longindexedfield_values (
    longindexedfield_id integer NOT NULL,
    "values" bigint
);




CREATE TABLE IF NOT EXISTS resource (
    id character varying(255) NOT NULL,
    creation_date timestamp without time zone NOT NULL,
    modification_date timestamp without time zone NOT NULL,
    payload text NOT NULL,
    payloadformat character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    fk_name character varying(255) NOT NULL
);



CREATE TABLE IF NOT EXISTS resourcetype (
    name character varying(255) NOT NULL,
    aliasgroup character varying(255),
    creation_date timestamp without time zone NOT NULL,
    indexmapperclass character varying(255),
    modification_date timestamp without time zone NOT NULL,
    payloadtype character varying(255) NOT NULL,
    schema text NOT NULL,
    schemaurl character varying(255)
);



CREATE TABLE IF NOT EXISTS public.resourcetype_aliases (
    resourcetype_name character varying(255) NOT NULL,
    aliases character varying(255)
);


CREATE TABLE IF NOT EXISTS resourcetype_indexfield (
    resourcetype_name character varying(255) NOT NULL,
    indexfields_resourcetype_name character varying(255) NOT NULL,
    indexfields_name character varying(255) NOT NULL
);


CREATE TABLE IF NOT EXISTS resourceversion (
    id character varying(255) NOT NULL,
    creation_date timestamp without time zone NOT NULL,
    parent_id character varying(255),
    payload text NOT NULL,
    resourcetype_name character varying(255),
    version character varying(255) NOT NULL,
    reference_id character varying(255),
    fk_name_version character varying(255)
);

CREATE TABLE IF NOT EXISTS schemadatabase (
    id character varying(255) NOT NULL,
    originalurl character varying(1000),
    schema text NOT NULL
);

CREATE TABLE IF NOT EXISTS stringindexedfield (
    id integer NOT NULL,
    name character varying(255),
    resource_id character varying(255)
);


CREATE TABLE IF NOT EXISTS stringindexedfield_values (
    stringindexedfield_id integer NOT NULL,
    "values" text
);


-- --
-- -- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE flyway_schema_history
--     ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);
--
--
-- --
-- -- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: vrasidas
-- --
--
-- CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history (success);-- USING btree (success);
--
--
-- --
-- -- Name: booleanindexedfield booleanindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE booleanindexedfield
--     ADD CONSTRAINT booleanindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: dateindexedfield dateindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE dateindexedfield
--     ADD CONSTRAINT dateindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: floatindexedfield floatindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE floatindexedfield
--     ADD CONSTRAINT floatindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: indexfield indexfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE indexfield
--     ADD CONSTRAINT indexfield_pkey PRIMARY KEY (resourcetype_name, name);
--
--
-- --
-- -- Name: integerindexedfield integerindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE integerindexedfield
--     ADD CONSTRAINT integerindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: longindexedfield longindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE longindexedfield
--     ADD CONSTRAINT longindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: resource resource_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resource
--     ADD CONSTRAINT resource_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: resourcetype resourcetype_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourcetype
--     ADD CONSTRAINT resourcetype_pkey PRIMARY KEY (name);
--
--
-- --
-- -- Name: resourceversion resourceversion_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourceversion
--     ADD CONSTRAINT resourceversion_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: schemadatabase schemadatabase_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE schemadatabase
--     ADD CONSTRAINT schemadatabase_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: stringindexedfield stringindexedfield_pkey; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE stringindexedfield
--     ADD CONSTRAINT stringindexedfield_pkey PRIMARY KEY (id);
--
--
-- --
-- -- Name: resourcetype_indexfield uk_iq7il1d8r9su5u0d7jcq3p4ug; Type: CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourcetype_indexfield
--     ADD CONSTRAINT uk_iq7il1d8r9su5u0d7jcq3p4ug UNIQUE (indexfields_resourcetype_name, indexfields_name);
--
--
-- --
-- -- Name: floatindexedfield_values fk6v8gu63ovpqbxsk10asfg8oq2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE floatindexedfield_values
--     ADD CONSTRAINT fk6v8gu63ovpqbxsk10asfg8oq2 FOREIGN KEY (floatindexedfield_id) REFERENCES floatindexedfield(id);
--
--
-- --
-- -- Name: resourcetype_indexfield fk7sik7aiiq0v42m8coyppdowsg; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourcetype_indexfield
--     ADD CONSTRAINT fk7sik7aiiq0v42m8coyppdowsg FOREIGN KEY (resourcetype_name) REFERENCES resourcetype(name);
--
--
-- --
-- -- Name: resource fk90kvqr4idlat5pee5tamehfpu; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resource
--     ADD CONSTRAINT fk90kvqr4idlat5pee5tamehfpu FOREIGN KEY (fk_name) REFERENCES resourcetype(name);
--
--
-- --
-- -- Name: dateindexedfield_values fk9a3s750lj9iu60jbm2twwt9p6; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE dateindexedfield_values
--     ADD CONSTRAINT fk9a3s750lj9iu60jbm2twwt9p6 FOREIGN KEY (dateindexedfield_id) REFERENCES dateindexedfield(id);
--
--
-- --
-- -- Name: booleanindexedfield fk_1fkd81c5w5hes5rw5y47d9rb2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE booleanindexedfield
--     ADD CONSTRAINT fk_1fkd81c5w5hes5rw5y47d9rb2 FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: floatindexedfield fk_6c41agke54vxn511raoxrprf1; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE floatindexedfield
--     ADD CONSTRAINT fk_6c41agke54vxn511raoxrprf1 FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: integerindexedfield fk_7wo5ce26ppipju2al2yj2mgrh; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE integerindexedfield
--     ADD CONSTRAINT fk_7wo5ce26ppipju2al2yj2mgrh FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: longindexedfield fk_g768fvk33224p605911uce1vm; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE longindexedfield
--     ADD CONSTRAINT fk_g768fvk33224p605911uce1vm FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: dateindexedfield fk_hcnmu0kxpthc8fy42vrdnx1gd; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE dateindexedfield
--     ADD CONSTRAINT fk_hcnmu0kxpthc8fy42vrdnx1gd FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: stringindexedfield fk_pqi7pbaye7s6mffq25irom10q; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE stringindexedfield
--     ADD CONSTRAINT fk_pqi7pbaye7s6mffq25irom10q FOREIGN KEY (resource_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: resourcetype_indexfield fkd89saslxgqkjk9xe5le8xya66; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourcetype_indexfield
--     ADD CONSTRAINT fkd89saslxgqkjk9xe5le8xya66 FOREIGN KEY (indexfields_resourcetype_name, indexfields_name) REFERENCES indexfield(resourcetype_name, name);
--
--
-- --
-- -- Name: stringindexedfield_values fkekj0s114ihg96iftnijf668x2; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE stringindexedfield_values
--     ADD CONSTRAINT fkekj0s114ihg96iftnijf668x2 FOREIGN KEY (stringindexedfield_id) REFERENCES stringindexedfield(id);
--
--
-- --
-- -- Name: indexfield fkhv3nafd941tssde6y2vh8cwx0; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE indexfield
--     ADD CONSTRAINT fkhv3nafd941tssde6y2vh8cwx0 FOREIGN KEY (resourcetype_name) REFERENCES resourcetype(name);
--
--
-- --
-- -- Name: resourceversion fkn0tq8akvjeyjk7v9obhu56s6m; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourceversion
--     ADD CONSTRAINT fkn0tq8akvjeyjk7v9obhu56s6m FOREIGN KEY (fk_name_version) REFERENCES resourcetype(name);
--
--
-- --
-- -- Name: longindexedfield_values fko3pakurnjvq9r24qffry92hcc; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE longindexedfield_values
--     ADD CONSTRAINT fko3pakurnjvq9r24qffry92hcc FOREIGN KEY (longindexedfield_id) REFERENCES longindexedfield(id);
--
--
-- --
-- -- Name: booleanindexedfield_values fkol4dou8gqgg0ik8yd85usenf5; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE booleanindexedfield_values
--     ADD CONSTRAINT fkol4dou8gqgg0ik8yd85usenf5 FOREIGN KEY (booleanindexedfield_id) REFERENCES booleanindexedfield(id);
--
--
-- --
-- -- Name: resourceversion fkrq94kmfewfixdnhss7vhp5d2f; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE resourceversion
--     ADD CONSTRAINT fkrq94kmfewfixdnhss7vhp5d2f FOREIGN KEY (reference_id) REFERENCES resource(id);
--
--
-- --
-- -- Name: integerindexedfield_values fksuxndi576ylkdfs6h6afr6ckx; Type: FK CONSTRAINT; Schema: public; Owner: vrasidas
-- --
--
-- ALTER TABLE integerindexedfield_values
--     ADD CONSTRAINT fksuxndi576ylkdfs6h6afr6ckx FOREIGN KEY (integerindexedfield_id) REFERENCES integerindexedfield(id);
--
--

--
-- PostgreSQL database dump complete
--

