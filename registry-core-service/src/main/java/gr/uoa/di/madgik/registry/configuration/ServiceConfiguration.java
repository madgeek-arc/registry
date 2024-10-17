package gr.uoa.di.madgik.registry.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

@Configuration(proxyBeanMethods = false)
@EnableAspectJAutoProxy
public class ServiceConfiguration {

    @Primary
    @Bean
    public TaskExecutor taskExecutor() {
        VirtualThreadTaskExecutor executor = new VirtualThreadTaskExecutor();
        return executor;
    }

}
