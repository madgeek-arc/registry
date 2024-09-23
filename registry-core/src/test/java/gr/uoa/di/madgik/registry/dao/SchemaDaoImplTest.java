package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.MockDatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.Schema;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import jakarta.transaction.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
@ComponentScan("gr.uoa.di.madgik.registry.dao")
public class SchemaDaoImplTest {

    private static Logger logger = LoggerFactory.getLogger(SchemaDaoImplTest.class);

    @Autowired
    SchemaDao schemaDao;

    private Schema testingSchema;


    @Before
    public void initialize() {
        testingSchema = schemaDao.getSchema("cccbd2ae2abfd0bb0d1c6c2216116ed1");
    }

    @Test
    public void getSchema_OK() {
        Assert.assertEquals(schemaDao.getSchema("cccbd2ae2abfd0bb0d1c6c2216116ed1"), testingSchema);
    }

    @Test
    public void getSchema_NOT_FOUND() {
        Assert.assertNull(schemaDao.getSchema("cccbd2ae2abfd0bb0d1c6c2216116ed2"));
    }

    @Test
    public void getSchemaByUrl_OK() {
        Assert.assertEquals(schemaDao.getSchemaByUrl("employee"), testingSchema);
    }

    @Test
    public void getSchemaByUrl_NOT_FOUND() {
        Assert.assertNull(schemaDao.getSchemaByUrl("event"));
    }

    @Test
    public void addSchema_OK() {
        Schema schema = new Schema();
        schema.setId("aoubla");
        schema.setOriginalUrl("original_you_are_l");
        schema.setSchema("schema");
        schemaDao.addSchema(schema);
        Assert.assertEquals(schemaDao.getSchemaByUrl("original_you_are_l"), schema);
    }

    @Test
    public void deleteSchema_OK() {
        schemaDao.deleteSchema(testingSchema);
        Assert.assertNull(schemaDao.getSchemaByUrl("employee"));
    }


}
