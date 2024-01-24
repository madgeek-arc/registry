package eu.openminted.registry.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.service.ResourceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Date;

@Configuration
public class ResourceTypeInit {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeInit.class);

    private final ResourceLoader resourceLoader;
    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper = new ObjectMapper();


    public ResourceTypeInit(ResourceLoader resourceLoader, ResourceTypeService resourceTypeService) {
        this.resourceLoader = resourceLoader;
        this.resourceTypeService = resourceTypeService;
    }

    @PostConstruct
    void addResourceTypes() throws IOException {
        Resource[] resources = loadResources("classpath:resourceTypes/*.json");
        for (Resource resource : resources) {
            try {
                addResourceTypeFromFile(resource);
            } catch (IOException e) {
                logger.error(String.format("Could not add Resource Type from file [filename=%s]", resource.getFilename()), e);
            }
        }
    }

    /**
     * Get an array of files as {@link Resource Resources}.
     *
     * @param pattern The pattern to check for files.
     * @return
     * @throws IOException
     */
    private Resource[] loadResources(String pattern) throws IOException {
        return ResourcePatternUtils.getResourcePatternResolver(resourceLoader).getResources(pattern);
    }

    /**
     * Reads {@link ResourceType} from {@link Resource} and adds it if not exists.
     *
     * @param resource The resource containing the {@link ResourceType}.
     * @throws IOException
     */
    private void addResourceTypeFromFile(Resource resource) throws IOException {
        ResourceType resourceType = mapper.readValue(resource.getInputStream(), ResourceType.class);
        if (resourceTypeService.getResourceType(resourceType.getName()) == null) {
            logger.info(String.format("Adding [resourceType=%s]", resourceType.getName()));
            resourceType.setCreationDate(new Date());
            resourceType.setModificationDate(new Date());
            resourceTypeService.addResourceType(resourceType);
        } else {
            logger.info(String.format("Found [resourceType=%s]", resourceType.getName()));
        }
    }
}
