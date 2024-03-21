package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.MockDatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
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

import javax.persistence.PersistenceException;
import javax.transaction.Transactional;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
@ComponentScan("gr.uoa.di.madgik.registry.dao")
public class ResourceDaoImplTest {

    private static Logger logger = LoggerFactory.getLogger(ResourceDaoImplTest.class);

    @Autowired
    ResourceDao resourceDao;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    @Autowired
    IndexedFieldDao indexedFieldDao;

    private ResourceType testingResourceType;

    private Resource testingResource;

    @Before
    public void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
        testingResource = resourceDao.getResource().get(0);
    }


    @Test
    public void getResource_OK() {
        Resource resource = resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
        Assert.assertEquals(resource, testingResource);
    }

    @Test
    public void getResource_WRONG_ID() {
        Resource resource = resourceDao.getResource("f98db949-f3e3-4d30-9894-7dd2e291fbef");
        Assert.assertNotEquals(resource, testingResource);
    }

    @Test
    public void getModifiedSince_OK() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-18 15:59:22.122");
        List<Resource> resources = resourceDao.getModifiedSince(date);
        Assert.assertNotEquals(resources.size(), 0);

        Assert.assertEquals(resources.get(0), testingResource);
    }

    @Test
    public void getModifiedSince_NOTHING_MODIFIED_SINCE() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-20 15:59:22.122");
        List<Resource> resources = resourceDao.getModifiedSince(date);
        Assert.assertEquals(resources.size(), 0);
    }

    @Test
    public void getCreatedSince_OK() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-18 15:59:22.122");
        List<Resource> resources = resourceDao.getCreatedSince(date);
        Assert.assertNotEquals(resources.size(), 0);

        Assert.assertEquals(resources.get(0), testingResource);
        return;
    }

    @Test
    public void getCreatedSince_NOTHING_CREATED_SINCE() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-20 15:59:22.122");
        List<Resource> resources = resourceDao.getCreatedSince(date);
        Assert.assertEquals(resources.size(), 0);
    }

    @Test
    public void getResourceByResourceType_OK() {
        Assert.assertNotEquals(resourceDao.getResource(testingResourceType).size(), 0);
    }

    @Test
    public void getResourceByResourceType_NO_RESOURCETYPE_FOUND() {
        Assert.assertEquals(resourceDao.getResource(resourceTypeDao.getResourceType("event")).size(), 0);
    }

    @Test
    public void getResourceByResourceTypeFromTo_OK() {
        Assert.assertNotEquals(resourceDao.getResource(testingResourceType, 0, 10).size(), 0);
    }

    @Test
    public void getResourceByResourceTypeFromTo_OUT_OF_RANGE() {
        Assert.assertEquals(resourceDao.getResource(testingResourceType, 2, 10).size(), 0);
    }

    @Test
    public void getResourcesFromTo_OK() {
        Assert.assertNotEquals(resourceDao.getResource(0, 10).size(), 0);
    }

    @Test
    public void getResourcesFromTo_OUT_OF_RANGE() {
        Assert.assertEquals(resourceDao.getResource(2, 10).size(), 0);
    }

    @Test
    public void addResource_OK() {

        Resource resource = new Resource();
        resource.setId("12345");
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayload("test payload");
        resource.setResourceType(testingResourceType);
        resource.setPayloadFormat("xml");
        resource.setPayloadUrl("not_set");

        resourceDao.addResource(resource);

        Assert.assertEquals(resourceDao.getResource().size(), 2);
    }

    @Test(expected = PersistenceException.class)
    public void addResource_NO_RESOURCETYPE() {

        Resource resource = new Resource();
        resource.setId("12345");
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayload("test payload");
        resource.setPayloadFormat("xml");
        resource.setPayloadUrl("not_set");

        resourceDao.addResource(resource);

    }

    @Test
    public void updateResource_OK() {
        testingResource.setPayload("Updated payload");
        resourceDao.updateResource(testingResource);

        Resource resource = resourceDao.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");

        Assert.assertEquals(resource, testingResource);

    }

    @Test
    public void deleteResource() {
        resourceDao.deleteResource(testingResource);
        Assert.assertEquals(resourceDao.getResource().size(), 0);
    }
}
