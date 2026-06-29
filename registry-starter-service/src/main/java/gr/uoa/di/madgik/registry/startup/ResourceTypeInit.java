/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.startup;

import tools.jackson.databind.ObjectMapper;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import gr.uoa.di.madgik.registry.domain.index.SearchCapability;
import gr.uoa.di.madgik.registry.service.ResourceTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResourceTypeInit implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ResourceTypeInit.class);

    private final ResourceTypeService resourceTypeService;
    private final ObjectMapper mapper;
    private final String resourceTypesLocation;
    private final boolean refresh;


    public ResourceTypeInit(@Value("${registry.resource-type-init.location:classpath*:resourceTypes}") String resourceTypesLocation,
                            @Value("${registry.resource-type-init.refresh:false}") boolean refresh,
                            ResourceTypeService resourceTypeService,
                            ObjectMapper objectMapper) {
        this.resourceTypesLocation = resourceTypesLocation;
        this.refresh = refresh;
        this.resourceTypeService = resourceTypeService;
        this.mapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
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
     * Reads {@link ResourceType} from {@link Resource} and adds or updates it based on content hash.
     *
     * @param resource The resource containing the {@link ResourceType}.
     * @throws IOException
     */
    private void addResourceTypeFromFile(Resource resource) throws IOException {
        ResourceType fromFile = mapper.readValue(resource.getInputStream(), ResourceType.class);
        ResourceType existing = resourceTypeService.getResourceType(fromFile.getName());

        if (existing == null) {
            logger.info("Adding [resourceType={}]", fromFile.getName());
            fromFile.setCreationDate(Instant.now());
            fromFile.setModificationDate(Instant.now());
            resourceTypeService.addResourceType(fromFile);
        } else if (refresh && !contentHash(existing).equals(contentHash(fromFile))) {
            logger.info("Updating [resourceType={}]", fromFile.getName());
            fromFile.setCreationDate(existing.getCreationDate());
            fromFile.setModificationDate(Instant.now());
            resourceTypeService.updateResourceType(fromFile);
        } else {
            logger.debug("Found [resourceType={}]", existing.getName());
        }
    }

    private String contentHash(ResourceType rt) {
        String json = mapper.writeValueAsString(ResourceTypeContent.of(rt));
        return sha256Hex(json);
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // --- canonical content records (metadata fields excluded) ---

    private record IndexFieldContent(
            String name,
            String path,
            String type,
            String label,
            String defaultValue,
            boolean multivalued,
            boolean primaryKey,
            Set<SearchCapability> searchCapabilities,
            float embeddingWeight,
            String relatedResourceType,
            String relatedResourceTypeField
    ) {
        static IndexFieldContent of(IndexField f) {
            return new IndexFieldContent(
                    f.getName(),
                    f.getPath(),
                    f.getType(),
                    f.getLabel(),
                    f.getDefaultValue(),
                    f.isMultivalued(),
                    f.isPrimaryKey(),
                    f.getSearchCapabilities(),
                    f.getEmbeddingWeight(),
                    f.getRelatedResourceType(),
                    f.getRelatedResourceTypeField()
            );
        }
    }

    private record ResourceTypeContent(
            String name,
            String schema,
            String schemaUrl,
            String payloadType,
            String indexMapperClass,
            List<IndexFieldContent> indexFields,
            SortedSet<String> aliases,
            SortedMap<String, String> properties
    ) {
        static ResourceTypeContent of(ResourceType rt) {
            List<IndexFieldContent> fields = rt.getIndexFields() == null ? List.of() :
                    rt.getIndexFields().stream()
                            .sorted(Comparator.comparing(f -> f.getName() != null ? f.getName() : ""))
                            .map(IndexFieldContent::of)
                            .toList();
            return new ResourceTypeContent(
                    rt.getName(),
                    rt.getSchema(),
                    rt.getSchemaUrl(),
                    rt.getPayloadType(),
                    rt.getIndexMapperClass(),
                    fields,
                    rt.getAliases() != null ? new TreeSet<>(rt.getAliases()) : new TreeSet<>(),
                    rt.getProperties() != null ? new TreeMap<>(rt.getProperties()) : new TreeMap<>()
            );
        }
    }
}
