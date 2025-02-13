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

import gr.uoa.di.madgik.registry.dao.IndexedFieldDao;
import gr.uoa.di.madgik.registry.domain.index.IndexedField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service("indexedFieldService")
public class IndexedFieldServiceImpl implements IndexedFieldService {

    private static Logger logger = LoggerFactory.getLogger(IndexedFieldServiceImpl.class);

    private final IndexedFieldDao indexedFieldDao;
    private final ResourceService resourceService;

    public IndexedFieldServiceImpl(IndexedFieldDao indexedFieldDao, ResourceService resourceService) {
        this.indexedFieldDao = indexedFieldDao;
        this.resourceService = resourceService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndexedField> getIndexedFields(String resourceId) {
        return indexedFieldDao.getIndexedFieldsOfResource(resourceService.getResource(resourceId));
    }

}

