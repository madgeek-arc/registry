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

package gr.uoa.di.madgik.registry.index;

import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Repository;

/**
 * Created by antleb on 5/21/16.
 */
@Repository("indexMapperFactory")
public class IndexMapperFactory {

    private final ApplicationContext context;

    public IndexMapperFactory(ApplicationContext context) {
        this.context = context;
    }

    public IndexMapper createIndexMapper(ResourceType resourceType) throws Exception {

        IndexMapper indexMapper = (IndexMapper) context.getBean(Class.forName(resourceType.getIndexMapperClass()));

        if (DefaultIndexMapper.class.isAssignableFrom(Class.forName(resourceType.getIndexMapperClass())))
            ((DefaultIndexMapper) indexMapper).setIndexFields(resourceType.getIndexFields());

        return indexMapper;
    }


}
