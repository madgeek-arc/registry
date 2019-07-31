package eu.openminted.registry.core.dao;

import configuration.MockDatabaseConfiguration;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
@ComponentScan("eu.openminted.registry.core.dao")
public class IndexedFieldDaoImplTest {

    private static Logger logger = LogManager.getLogger(IndexedFieldDaoImplTest.class);

    @Autowired
    IndexedFieldDao indexedFieldDao;

    @Autowired
    ResourceDao resourceDao;

    private Resource testingResource;


    @Before
    public void initialize(){
        testingResource = resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
    }


    @Test
    public void getIndexedFields_OK() {
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(testingResource);
        Assert.assertEquals(indexedFields.size(),6);
    }

    @Test
    public void getIndexedFields_NONE() {
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbeg"));
        Assert.assertNotEquals(indexedFields.size(),6);
    }

    @Test
    public void deleteIndexedField_OK(){
        indexedFieldDao.deleteAllIndexedFields(testingResource);
        Assert.assertEquals(indexedFieldDao.getIndexedFieldsOfResource(testingResource).size(),0);
    }



}