/**
 * Copyright 2018-2025 OpenAIRE AMKE & Athena Research and Innovation Center
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gr.uoa.di.madgik.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

@Component
public class ResourceTypeInit {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeInit.class);

    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String resourceTypesLocation;


    public ResourceTypeInit(@Value("${registry.resource-type-init.location:classpath:resourceTypes}") String resourceTypesLocation,
                            ResourceTypeService resourceTypeService) {
        this.resourceTypesLocation = resourceTypesLocation;
        this.resourceTypeService = resourceTypeService;
    }

    @PostConstruct
    void addResourceTypes() {
        String pattern = getLocationPattern();
        Resource[] resources = loadResources(pattern);
        if (resources == null || resources.length == 0) {
            logger.info("No Resource Types found under '{}'", pattern);
        } else {
            for (Resource resource : resources) {
                try {
                    addResourceTypeFromFile(resource);
                } catch (IOException e) {
                    logger.error(String.format("Could not add Resource Type from file [filename=%s]", resource.getFilename()), e);
                } catch (Exception e) {
                    logger.error("Invalid format of resourceType '{}'", resource.getFilename(), e);
                }
            }
        }
    }

    /**
     * <p>Creates the location to search for ResourceType (.json) files.</p>
     * <p>Makes sure to check for JSON file and removes duplicate // from the path.</p>
     *
     * @return
     */
    private String getLocationPattern() {
        String location = resourceTypesLocation;
        if (!resourceTypesLocation.endsWith(".json"))
            location += "/*.json";
        return location.replaceAll("/{2,}", "/");
    }

    /**
     * Get an array of files as {@link Resource Resources}.
     *
     * @param pattern The pattern to check for files.
     * @return
     * @throws IOException
     */
    private Resource[] loadResources(String pattern) {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            return resolver.getResources(pattern);
        } catch (IOException e) {
            logger.error("Could not load resourceTypes from '{}'", pattern, e);
        }
        return null;
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
            logger.info("Adding [resourceType={}]", resourceType.getName());
            resourceType.setCreationDate(new Date());
            resourceType.setModificationDate(new Date());
            resourceTypeService.addResourceType(resourceType);
        } else {
            logger.debug("Found [resourceType={}]", resourceType.getName());
        }
    }
}
