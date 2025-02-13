--
-- PostgreSQL database dump
--

-- Dumped from database version 14.15 (Ubuntu 14.15-0ubuntu0.22.04.1)
-- Dumped by pg_dump version 14.15 (Ubuntu 14.15-0ubuntu0.22.04.1)


--
-- Name: booleanindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.booleanindexedfield (
                                                          id integer NOT NULL,
                                                          name character varying(255),
                                                          resource_id character varying(255)
);

--
-- Name: booleanindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.booleanindexedfield_values (
                                                                 booleanindexedfield_id integer NOT NULL,
                                                                 "values" boolean
);

--
-- Name: resource; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resource (
                                               id character varying(255) NOT NULL,
                                               creation_date timestamp(6) without time zone NOT NULL,
                                               modification_date timestamp(6) without time zone NOT NULL,
                                               payload text NOT NULL,
                                               payloadformat character varying(255) NOT NULL,
                                               version character varying(255) NOT NULL,
                                               fk_name character varying(255) NOT NULL
);

--
-- Name: dateindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.dateindexedfield (
                                                       id integer NOT NULL,
                                                       name character varying(255),
                                                       resource_id character varying(255)
);

--
-- Name: dateindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.dateindexedfield_values (
                                                              dateindexedfield_id integer NOT NULL,
                                                              "values" timestamp(6) without time zone
);

--
-- Name: floatindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.floatindexedfield (
                                                        id integer NOT NULL,
                                                        name character varying(255),
                                                        resource_id character varying(255)
);

--
-- Name: floatindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.floatindexedfield_values (
                                                               floatindexedfield_id integer NOT NULL,
                                                               "values" double precision
);

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.flyway_schema_history (
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

--
-- Name: indexedfield_seq; Type: SEQUENCE; Schema: public; Owner: admin
--

CREATE SEQUENCE IF NOT EXISTS indexedfield_seq
    START WITH 1
    INCREMENT BY 50;

--
-- Name: indexfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.indexfield (
                                                 name character varying(255) NOT NULL,
                                                 defaultvalue character varying(255),
                                                 label character varying(255),
                                                 multivalued boolean,
                                                 path character varying(255),
                                                 primarykey boolean,
                                                 type character varying(255),
                                                 resourcetype_name character varying(255) NOT NULL
);

--
-- Name: integerindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.integerindexedfield (
                                                          id integer NOT NULL,
                                                          name character varying(255),
                                                          resource_id character varying(255)
);

--
-- Name: integerindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.integerindexedfield_values (
                                                                 integerindexedfield_id integer NOT NULL,
                                                                 "values" bigint
);

--
-- Name: longindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.longindexedfield (
                                                       id integer NOT NULL,
                                                       name character varying(255),
                                                       resource_id character varying(255)
);

--
-- Name: longindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.longindexedfield_values (
                                                              longindexedfield_id integer NOT NULL,
                                                              "values" bigint
);

--
-- Name: resourcetype; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resourcetype (
                                                   name character varying(255) NOT NULL,
                                                   creation_date timestamp(6) without time zone NOT NULL,
                                                   indexmapperclass character varying(255),
                                                   modification_date timestamp(6) without time zone NOT NULL,
                                                   payloadtype character varying(255) NOT NULL,
                                                   schema text NOT NULL,
                                                   schemaurl character varying(255)
);

--
-- Name: resourcetype_aliases; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resourcetype_aliases (
                                                           resourcetype_name character varying(255) NOT NULL,
                                                           aliases character varying(255)
);

--
-- Name: resourcetype_indexfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resourcetype_indexfield (
                                                              resourcetype_name character varying(255) NOT NULL,
                                                              indexfields_name character varying(255) NOT NULL,
                                                              indexfields_resourcetype_name character varying(255) NOT NULL
);

--
-- Name: resourcetype_properties; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resourcetype_properties (
                                                              resourcetype_name character varying(255) NOT NULL,
                                                              properties character varying(255),
                                                              properties_key character varying(255) NOT NULL
);

--
-- Name: resourceversion; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.resourceversion (
                                                      id character varying(255) NOT NULL,
                                                      creation_date timestamp(6) without time zone NOT NULL,
                                                      parent_id character varying(255),
                                                      payload text NOT NULL,
                                                      resourcetype_name character varying(255),
                                                      version character varying(255) NOT NULL,
                                                      reference_id character varying(255),
                                                      fk_name_version character varying(255)
);

--
-- Name: schemadatabase; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.schemadatabase (
                                                     id character varying(255) NOT NULL,
                                                     originalurl character varying(1000),
                                                     schema text NOT NULL
);

--
-- Name: stringindexedfield; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.stringindexedfield (
                                                         id integer NOT NULL,
                                                         name character varying(255),
                                                         resource_id character varying(255)
);

--
-- Name: stringindexedfield_values; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE IF NOT EXISTS public.stringindexedfield_values (
                                                                stringindexedfield_id integer NOT NULL,
                                                                "values" text
);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: admin
--

CREATE INDEX flyway_schema_history_s_idx ON flyway_schema_history (success);


--
-- PostgreSQL database dump complete
--

