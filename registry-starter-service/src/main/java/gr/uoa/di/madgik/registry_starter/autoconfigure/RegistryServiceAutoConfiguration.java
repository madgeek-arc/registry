package gr.uoa.di.madgik.registry_starter.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan({
        "gr.uoa.di.madgik.registry",
        "gr.uoa.di.madgik.registry.controllers"
})
public class RegistryServiceAutoConfiguration {

//    @Bean
//    @ConditionalOnMissingBean(ResourceDao.class)
//    public ResourceDao resourceDao() {
//        return new ResourceDaoImpl();
//    }
//
//    @Bean
//    @ConditionalOnMissingBean(ResourceService.class)
//    @ConditionalOnBean(value = {
//            ResourceDao.class,
//            ResourceTypeDao.class,
//            IndexMapperFactory.class,
//            IndexedFieldDao.class,
//            ResourceValidator.class
//    })
//    ResourceService resourceService(ResourceDao resourceDao, ResourceTypeDao resourceTypeDao,
//                                    IndexMapperFactory indexMapperFactory, IndexedFieldDao indexedFieldDao,
//                                    ResourceValidator resourceValidator) {
//        return new ResourceServiceImpl(resourceDao, resourceTypeDao, indexMapperFactory, indexedFieldDao, resourceValidator);
//    }
}
