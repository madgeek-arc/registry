package eu.openminted.registry.core.dao;

import configuration.MockDatabaseConfiguration;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.domain.index.IndexField;
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
@ComponentScan("eu.openminted.registry.core.dao")
public class IndexFieldDaoImplTest {

    private static Logger logger = LoggerFactory.getLogger(IndexFieldDaoImplTest.class);

    @Autowired
    IndexFieldDao indexFieldDao;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @Before
    public void initialize(){
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }


    @Test
    public void getIndexFields_OK() {
        List<IndexField> indexFields = indexFieldDao.getIndexFieldsOfResourceType(testingResourceType);
        Assert.assertEquals(indexFields.size(),6);
    }

    @Test
    public void getIndexFields_NONE() {
        List<IndexField> indexFields = indexFieldDao.getIndexFieldsOfResourceType(resourceTypeDao.getResourceType("event"));
        Assert.assertNotEquals(indexFields.size(),6);
    }

}
