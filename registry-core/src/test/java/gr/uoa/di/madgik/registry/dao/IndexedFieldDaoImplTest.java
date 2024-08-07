package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.MockDatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
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

import javax.transaction.Transactional;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
@ComponentScan("gr.uoa.di.madgik.registry.dao")
public class IndexedFieldDaoImplTest {

    private static Logger logger = LoggerFactory.getLogger(IndexedFieldDaoImplTest.class);

    @Autowired
    IndexedFieldDao indexedFieldDao;

    @Autowired
    ResourceDao resourceDao;

    private Resource testingResource;


    @Before
    public void initialize() {
        testingResource = resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
    }


    @Test
    public void getIndexedFields_OK() {
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(testingResource);
        Assert.assertEquals(indexedFields.size(), 6);
    }

    @Test
    public void getIndexedFields_NONE() {
        List<IndexedField> indexedFields = indexedFieldDao.getIndexedFieldsOfResource(resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbeg"));
        Assert.assertNotEquals(indexedFields.size(), 6);
    }

    @Test
    public void deleteIndexedField_OK() {
        indexedFieldDao.deleteAllIndexedFields(testingResource);
        Assert.assertEquals(indexedFieldDao.getIndexedFieldsOfResource(testingResource).size(), 0);
    }


}
