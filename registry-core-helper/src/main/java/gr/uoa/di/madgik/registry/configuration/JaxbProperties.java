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

package gr.uoa.di.madgik.registry.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the registry JAXB autoconfiguration.
 *
 * <p>Bind these in your {@code application.yml}:
 * <pre>{@code
 * registry:
 *   jaxb:
 *     include-packages:
 *       - com.example.myapp.domain
 *       - com.example.myapp.generated
 * }</pre>
 *
 * <p>Every concrete class annotated with {@code @XmlRootElement} found in the listed packages
 * will be registered in the {@link jakarta.xml.bind.JAXBContext} bean. The context is created
 * once at startup and is shared by {@link gr.uoa.di.madgik.registry.service.ParserPool}.
 *
 * <p>This configuration only takes effect when the resource type's {@code payloadType} is
 * {@code "xml"}. JSON-only deployments do not need to set any of these properties.
 */
@ConfigurationProperties(prefix = "registry.jaxb")
public class JaxbProperties {

    /**
     * Packages to scan for {@code @XmlRootElement}-annotated classes.
     * All concrete (non-interface, non-abstract) classes found are registered in the JAXBContext.
     */
    private List<String> includePackages = new ArrayList<>();

    public List<String> getIncludePackages() {
        return includePackages;
    }

    public void setIncludePackages(List<String> includePackages) {
        this.includePackages = includePackages;
    }
}
