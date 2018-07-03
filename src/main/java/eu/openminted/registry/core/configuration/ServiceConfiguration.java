package eu.openminted.registry.core.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"eu.openminted.registry.core.service", "eu.openminted.registry.core.validation"})
public class ServiceConfiguration {
}
