package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.Resource;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_MISSING_RESOURCE_ID;
import static gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration.TEST_RESOURCE_ID;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceDaoImplTest {

    @Autowired
    ResourceDao resourceDao;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    @Autowired
    IndexedFieldDao indexedFieldDao;

    private ResourceType testingResourceType;

    private Resource testingResource;

    @BeforeAll
    void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
        testingResource = resourceDao.getResource().get(0);
    }


    @Test
    @Order(1)
    void getResource_OK() {
        Resource resource = resourceDao.getResource(TEST_RESOURCE_ID);
        Assertions.assertEquals(resource, testingResource);
    }

    @Test
    @Order(2)
    void getResource_WRONG_ID() {
        Resource resource = resourceDao.getResource(TEST_MISSING_RESOURCE_ID);
        Assertions.assertNotEquals(resource, testingResource);
    }

    @Test
    @Order(3)
    void getModifiedSince_OK() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-18 15:59:22.122");
        List<Resource> resources = resourceDao.getModifiedSince(date);
        Assertions.assertNotEquals(resources.size(), 0);

        Assertions.assertEquals(resources.get(0), testingResource);
    }

    @Test
    @Order(4)
    void getModifiedSince_NOTHING_MODIFIED_SINCE() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-20 15:59:22.122");
        List<Resource> resources = resourceDao.getModifiedSince(date);
        Assertions.assertEquals(resources.size(), 0);
    }

    @Test
    @Order(5)
    void getCreatedSince_OK() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-18 15:59:22.122");
        List<Resource> resources = resourceDao.getCreatedSince(date);
        Assertions.assertNotEquals(resources.size(), 0);
        Resource resource = resources.get(0);
        Assertions.assertEquals(resource, testingResource);
    }

    @Test
    @Order(6)
    void getCreatedSince_NOTHING_CREATED_SINCE() throws ParseException {
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse("2018-09-20 15:59:22.122");
        List<Resource> resources = resourceDao.getCreatedSince(date);
        Assertions.assertEquals(resources.size(), 0);
    }

    @Test
    @Order(7)
    void getResourceByResourceType_OK() {
        Assertions.assertNotEquals(resourceDao.getResource(testingResourceType).size(), 0);
    }

    @Test
    @Order(8)
    void getResourceByResourceType_NO_RESOURCETYPE_FOUND() {
        Assertions.assertThrows(NullPointerException.class, () -> resourceDao.getResource(resourceTypeDao.getResourceType("event")).size());
    }

    @Test
    @Order(9)
    void getResourceByResourceTypeFromTo_OK() {
        Assertions.assertNotEquals(resourceDao.getResource(testingResourceType, 0, 10).size(), 0);
    }

    @Test
    @Order(10)
    void getResourceByResourceTypeFromTo_OUT_OF_RANGE() {
        Assertions.assertEquals(resourceDao.getResource(testingResourceType, 2, 10).size(), 0);
    }

    @Test
    @Order(11)
    void getResourcesFromTo_OK() {
        Assertions.assertNotEquals(resourceDao.getResource(0, 10).size(), 0);
    }

    @Test
    @Order(12)
    void getResourcesFromTo_OUT_OF_RANGE() {
        Assertions.assertEquals(resourceDao.getResource(2, 10).size(), 0);
    }

    @Test
    @Order(13)
    void addResource_OK() {

        Resource resource = new Resource();
        resource.setId("12345");
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayload("test payload");
        resource.setResourceType(testingResourceType);
        resource.setPayloadFormat("xml");
        resource.setPayloadUrl("not_set");

        resourceDao.addResource(resource);

        Assertions.assertEquals(resourceDao.getResource().size(), 2);
    }

    @Test
    @Order(14)
    void addResource_NO_RESOURCETYPE() {

        Resource resource = new Resource();
        resource.setId("12345");
        resource.setCreationDate(new Date());
        resource.setModificationDate(new Date());
        resource.setPayload("test payload");
        resource.setPayloadFormat("xml");
        resource.setPayloadUrl("not_set");

        Assertions.assertThrows(PersistenceException.class, () -> resourceDao.addResource(resource));

    }

    @Test
    @Order(15)
    void updateResource_OK() {
        testingResource.setPayload("Updated payload");
        resourceDao.updateResource(testingResource);

        Resource resource = resourceDao.getResource(TEST_RESOURCE_ID);

        Assertions.assertEquals(resource.getPayload(), testingResource.getPayload());

    }

    @Test
    @Order(16)
    void deleteResource() {
        resourceDao.deleteResource(resourceDao.getResource(TEST_RESOURCE_ID));
        Assertions.assertEquals(resourceDao.getResource().size(), 0);
    }
}
