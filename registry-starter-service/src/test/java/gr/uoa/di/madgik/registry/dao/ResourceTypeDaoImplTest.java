package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceTypeDaoImplTest {

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @BeforeAll
    void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }

    @Test
    @Order(1)
    void getResourceTypeIndexFields_OK() {
        Assertions.assertEquals(resourceTypeDao.getResourceTypeIndexFields("employee").size(), testingResourceType.getIndexFields().size());
    }

    @Test
    @Order(2)
    void getResourceTypeIndexFields_NOT_FOUND() {
        Assertions.assertNotEquals(resourceTypeDao.getResourceTypeIndexFields("event").toArray(), testingResourceType.getIndexFields().toArray());
    }

    @Test
    @Order(3)
    void deleteResourceType() {
        resourceTypeDao.deleteResourceType(testingResourceType.getName());
        Assertions.assertNull(resourceTypeDao.getResourceType("employee"));
    }


}
