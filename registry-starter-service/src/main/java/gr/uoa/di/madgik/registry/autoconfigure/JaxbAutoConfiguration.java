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

package gr.uoa.di.madgik.registry.autoconfigure;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot autoconfiguration that creates a {@link JAXBContext} bean by scanning the
 * packages listed in {@link JaxbProperties#getIncludePackages()} for classes annotated with
 * {@link XmlRootElement}.
 *
 * <h2>Activation</h2>
 * <p>This autoconfiguration is active whenever {@code jakarta.xml.bind.JAXBContext} is on the
 * classpath. It only creates the {@code JAXBContext} bean if no other bean of that type has
 * already been declared ({@code @ConditionalOnMissingBean}), so downstream applications can
 * provide their own if needed.
 *
 * <h2>Configuration</h2>
 * <pre>{@code
 * registry:
 *   jaxb:
 *     include-packages:
 *       - com.example.myapp.domain
 * }</pre>
 *
 * <h2>Class discovery</h2>
 * <p>Only classes annotated with {@code @XmlRootElement} are collected. Abstract classes and
 * interfaces are excluded. JAXB automatically discovers additional referenced types (e.g. classes
 * referenced via {@code @XmlElement} fields) from the registered root elements.
 *
 * <h2>Empty package list</h2>
 * <p>If {@code registry.jaxb.include-packages} is empty or not configured, an empty
 * {@code JAXBContext} is created. This is sufficient for JSON-only deployments where
 * {@code ParserPool} never invokes JAXB.
 *
 * @see JaxbProperties
 * @see gr.uoa.di.madgik.registry.service.ParserPool
 */
@AutoConfiguration
@ConditionalOnClass(JAXBContext.class)
@EnableConfigurationProperties(JaxbProperties.class)
public class JaxbAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JaxbAutoConfiguration.class);

    /**
     * Creates a {@link JAXBContext} from all {@code @XmlRootElement}-annotated classes found in
     * the configured packages.
     *
     * @param properties the JAXB configuration properties
     * @return a fully initialized {@code JAXBContext}
     * @throws JAXBException if context initialization fails for any of the discovered classes
     */
    @Bean
    @ConditionalOnMissingBean
    public JAXBContext jaxbContext(JaxbProperties properties) throws JAXBException {
        List<Class<?>> classes = scanForJaxbClasses(properties.getIncludePackages());
        if (classes.isEmpty()) {
            logger.info("No @XmlRootElement classes found — creating empty JAXBContext. "
                    + "XML payloads will not be available unless registry.jaxb.include-packages is configured.");
            return JAXBContext.newInstance();
        }
        logger.info("Creating JAXBContext with {} class(es) from package(s): {}",
                classes.size(), properties.getIncludePackages());
        if (logger.isDebugEnabled()) {
            classes.forEach(c -> logger.debug("  Registering JAXB class: {}", c.getCanonicalName()));
        }
        return JAXBContext.newInstance(classes.toArray(new Class[0]));
    }

    /**
     * Scans the given packages using Spring's classpath scanner and collects all concrete classes
     * annotated with {@link XmlRootElement}.
     *
     * @param packages the list of base packages to scan
     * @return the discovered classes, never {@code null}
     */
    private List<Class<?>> scanForJaxbClasses(List<String> packages) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(XmlRootElement.class));

        List<Class<?>> classes = new ArrayList<>();
        for (String pkg : packages) {
            scanner.findCandidateComponents(pkg).forEach(bd -> {
                try {
                    classes.add(Class.forName(bd.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    logger.warn("Could not load class '{}' during JAXB scan: {}",
                            bd.getBeanClassName(), e.getMessage());
                }
            });
        }
        return classes;
    }
}
