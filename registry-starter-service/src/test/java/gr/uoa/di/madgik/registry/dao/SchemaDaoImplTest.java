package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.Schema;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class SchemaDaoImplTest {

    public static final String TEST_SCHEMA_ID = "cccbd2ae2abfd0bb0d1c6c2216116ed1";
    public static final String TEST_MISSING_SCHEMA_ID = "not-existing-schema-id";

    @Autowired
    SchemaDao schemaDao;

    private Schema testingSchema;

    @BeforeEach
    void initialize() {
        testingSchema = schemaDao.getSchema(TEST_SCHEMA_ID);
    }

    @Test
    void getSchema_OK() {
        Assertions.assertEquals(schemaDao.getSchema(TEST_SCHEMA_ID), testingSchema);
    }

    @Test
    void getSchema_NOT_FOUND() {
        Assertions.assertNull(schemaDao.getSchema(TEST_MISSING_SCHEMA_ID));
    }

    @Test
    void getSchemaByUrl_OK() {
        Assertions.assertEquals(schemaDao.getSchemaByUrl("employee"), testingSchema);
    }

    @Test
    void getSchemaByUrl_NOT_FOUND() {
        Assertions.assertNull(schemaDao.getSchemaByUrl("event"));
    }

    @Test
    void addSchema_OK() {
        Schema schema = new Schema();
        schema.setId("aoubla");
        schema.setOriginalUrl("original_you_are_l");
        schema.setSchema("schema");
        schemaDao.addSchema(schema);
        Assertions.assertEquals(schemaDao.getSchemaByUrl("original_you_are_l"), schema);
    }

    @Test
    void deleteSchema_OK() {
        schemaDao.deleteSchema(testingSchema);
        Assertions.assertNull(schemaDao.getSchemaByUrl("employee"));
    }


}
