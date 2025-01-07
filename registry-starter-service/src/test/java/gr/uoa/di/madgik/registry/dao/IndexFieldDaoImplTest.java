package gr.uoa.di.madgik.registry.dao;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IndexFieldDaoImplTest {

    @Autowired
    IndexFieldDao indexFieldDao;

    @Autowired
    ResourceTypeDao resourceTypeDao;

    private ResourceType testingResourceType;


    @BeforeAll
    void initialize() {
        testingResourceType = resourceTypeDao.getResourceType("employee");
    }


    @Test
    void getIndexFields_OK() {
        List<IndexField> indexFields = indexFieldDao.getIndexFieldsOfResourceType(testingResourceType);
        Assertions.assertEquals(indexFields.size(), 6);
    }

    @Test
    void getIndexFields_NONE() {
        ResourceType resourceType = resourceTypeDao.getResourceType("event");
        Assertions.assertThrows(NullPointerException.class, () -> indexFieldDao.getIndexFieldsOfResourceType(resourceType));
    }

}
