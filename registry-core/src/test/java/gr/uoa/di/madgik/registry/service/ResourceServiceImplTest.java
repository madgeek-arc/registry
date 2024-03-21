package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.MockDatabaseConfiguration;
import gr.uoa.di.madgik.registry.dao.ResourceTypeDao;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.transaction.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        MockDatabaseConfiguration.class
})
@Transactional
public class ResourceServiceImplTest {

    private static Logger logger = LoggerFactory.getLogger(ResourceServiceImplTest.class);

    @Autowired
    private ResourceService resourceService;

    @Autowired
    private ResourceTypeDao resourceTypeDao;

    private Resource testingResource;

    private ResourceType testingResourceType;

    @Before
    public void initialize() {
        testingResource = resourceService.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    public void getResource_OK() {
        Resource resource = resourceService.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
        Assert.assertEquals(resource, testingResource);
    }

    @Test
    public void getResource_WRONG_ID() {
        Resource resource = resourceService.getResource("f98db949-f3e3-4d30-9894-7dd2e291fbef");
        Assert.assertNotEquals(resource, testingResource);
    }

    @Test
    public void getResourceByResourceType_OK() {
        Assert.assertNotEquals(resourceService.getResource(testingResourceType).size(), 0);
    }

    @Test
    public void getResourceByResourceType_NO_RESOURCETYPE_FOUND() {
        Assert.assertEquals(resourceService.getResource(resourceTypeDao.getResourceType("event")).size(), 0);
    }

    @Test
    public void getResourceByResourceTypeFromTo_OK() {
        Assert.assertNotEquals(resourceService.getResource(testingResourceType, 0, 10).size(), 0);
    }

    @Test
    public void getResourceByResourceTypeFromTo_OUT_OF_RANGE() {
        Assert.assertEquals(resourceService.getResource(testingResourceType, 2, 10).size(), 0);
    }

    @Test
    public void getResourcesFromTo_OK() {
        Assert.assertNotEquals(resourceService.getResource(0, 10).size(), 0);
    }

    @Test
    public void getResourcesFromTo_OUT_OF_RANGE() {
        Assert.assertEquals(resourceService.getResource(2, 10).size(), 0);
    }

    @Test(expected = ServiceException.class)
    // TODO: remove expected exception when IndexedFields values column is renamed
    public void addResource_OK() {

        Resource resource = new Resource();
        resource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Jomazor</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resource.setResourceTypeName("employee");
        resource.setPayloadFormat("xml");

        resourceService.addResource(resource);

        Assert.assertEquals(resourceService.getResource().size(), 2);
    }

    @Test(expected = ServiceException.class)
    public void addResource_NO_RESOURCETYPE() {

        Resource resource = new Resource();
        resource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Rallis & Polyxronopoulos</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resource.setPayloadFormat("xml");

        resourceService.addResource(resource);
    }

    public void updateResource_OK() {
        testingResource.setPayload("<?xml version=\"1.0\"?> " +
                "<employee> " +
                " <author>Makis Dimakis</author> " +
                " <age>28</age> " +
                " <single>false</single>" +
                " <birthday>645544821000</birthday>" +
                " <salary>1292.123</salary>" +
                " <amka>051417010293821</amka>" +
                "</employee>");
        resourceService.updateResource(testingResource);

        Resource resource = resourceService.getResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");

        Assert.assertEquals(resource, testingResource);

    }

    @Test
    public void changeResourceType_OK() {
        String resourceTypeName = "employee";
        Resource resource = resourceService.changeResourceType(testingResource, resourceTypeDao.getResourceType(resourceTypeName));
        Assert.assertEquals(resource.getResourceTypeName(), resourceTypeName);
    }

    @Test
    public void deleteResource() {
        resourceService.deleteResource("e98db949-f3e3-4d30-9894-7dd2e291fbef");
        Assert.assertEquals(resourceService.getResource().size(), 0);
    }

}
