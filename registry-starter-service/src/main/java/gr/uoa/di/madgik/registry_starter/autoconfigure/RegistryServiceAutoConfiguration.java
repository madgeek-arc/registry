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
