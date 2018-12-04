package eu.openminted.registry.core.dao;

import configuration.MockDatabaseConfiguration;
import eu.openminted.registry.core.domain.ResourceType;
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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
@ComponentScan("eu.openminted.registry.core.dao")
public class ResourceTypeDaoImplTest {

    private static Logger logger = LogManager.getLogger(ResourceTypeDaoImplTest.class);

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @Before
    public void initialize(){
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    public void getResourceTypeIndexFields_OK(){
        Assert.assertEquals(resourceTypeDao.getResourceTypeIndexFields("employee").size(), testingResourceType.getIndexFields().size());
    }

    @Test
    public void getResourceTypeIndexFields_NOT_FOUND(){
        Assert.assertNotEquals(resourceTypeDao.getResourceTypeIndexFields("event"),testingResourceType.getIndexFields());
    }

    @Test
    public void deleteResourceType(){
//        resourceTypeDao.deleteResourceType(testingResourceType);
//        Assert.assertNull(resourceTypeDao.getResourceType("employee"));
    }


}