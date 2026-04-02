package gr.uoa.di.madgik.registry.configuration;

import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = DatabaseConfiguration.class, properties = "spring.profiles.active=test")
@Transactional
class HibernateConfigurationSmokeTest extends PostgreSqlTestContainerSupport {

    @MockitoBean
    EmbeddingModel embeddingModel;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void entityManagerFactory_uses_jakarta_interface_proxy() {
        Assertions.assertNotNull(entityManagerFactory);
        Assertions.assertFalse(entityManagerFactory instanceof SessionFactory);
        Assertions.assertDoesNotThrow(() -> entityManagerFactory.createEntityManager().close());
    }
}
