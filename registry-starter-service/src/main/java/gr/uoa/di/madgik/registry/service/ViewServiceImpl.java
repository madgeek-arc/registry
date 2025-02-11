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

import gr.uoa.di.madgik.registry.dao.ViewDao;
import gr.uoa.di.madgik.registry.domain.ResourceType;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("viewService")
@Scope(proxyMode = ScopedProxyMode.INTERFACES)
@Transactional
public class ViewServiceImpl implements ViewService {

    private final ViewDao viewDao;

    public ViewServiceImpl(ViewDao viewDao) {
        this.viewDao = viewDao;
    }

    @Override
    public void createView(ResourceType resourceType) {
        viewDao.createView(resourceType);
    }

    @Override
    public void deleteView(String resourceType) {
        viewDao.deleteView(resourceType);
    }

}

