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

package gr.uoa.di.madgik.registry.service;

import gr.uoa.di.madgik.registry.dao.IndexFieldDao;
import gr.uoa.di.madgik.registry.domain.index.IndexField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexFieldService")
public class IndexFieldServiceImpl implements IndexFieldService {

    private static Logger logger = LoggerFactory.getLogger(IndexFieldServiceImpl.class);

    private final IndexFieldDao indexFieldDao;
    private final ResourceTypeService resourceTypeService;

    public IndexFieldServiceImpl(IndexFieldDao indexFieldDao, ResourceTypeService resourceTypeService) {
        this.indexFieldDao = indexFieldDao;
        this.resourceTypeService = resourceTypeService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexField> getIndexFields(String resourceTypeName) {
        return indexFieldDao.getIndexFieldsOfResourceType(resourceTypeService.getResourceType(resourceTypeName));
    }
}

