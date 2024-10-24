package gr.uoa.di.madgik.registry_starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan({"gr.uoa.di.madgik.registry"})
public class RegistryClientAutoConfiguration {
}
