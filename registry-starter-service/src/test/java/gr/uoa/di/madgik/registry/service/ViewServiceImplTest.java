package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.configuration.DatabaseConfiguration;
import gr.uoa.di.madgik.registry.configuration.PostgreSqlTestContainerSupport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewServiceImplTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    ResourceTypeProjectionService resourceTypeProjectionService;

    @Autowired
    ResourceTypeService resourceTypeService;

    @Autowired
    ViewService viewService;

    @BeforeAll
    void createEmployeeView() {
        viewService.createView(resourceTypeService.getResourceType("employee"));
    }

    @Test
    void fetchResourceIds_returnsIdsForKnownResourceType() {
        List<String> ids = resourceTypeProjectionService.fetchResourceIds("employee");

        assertEquals(List.of(DatabaseConfiguration.TEST_RESOURCE_ID), ids);
    }

    @Test
    void fetchResourceIds_rejectsUnknownResourceType() {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> resourceTypeProjectionService.fetchResourceIds("employee; DROP VIEW employee_view"));

        assertEquals("Unknown resource type 'employee; DROP VIEW employee_view'", exception.getMessage());
    }
}
