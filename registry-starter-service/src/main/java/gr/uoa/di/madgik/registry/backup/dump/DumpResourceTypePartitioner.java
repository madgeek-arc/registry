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

package gr.uoa.di.madgik.registry.backup.dump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Component
@StepScope
public class DumpResourceTypePartitioner implements Partitioner {

    private static final Logger logger = LoggerFactory.getLogger(DumpResourceTypePartitioner.class);
    @Value("#{jobExecutionContext}")
    Properties jobExecution;
    @Value("#{jobExecutionContext['addedResourceTypes']}")
    private List<String> resourceTypes;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        Map<String, ExecutionContext> versionMap = new HashMap<>(gridSize);
        for (String resourceType : resourceTypes) {
            ExecutionContext context = new ExecutionContext();
            context.put("resourceType", resourceType);
            versionMap.put(resourceType, context);
        }
        return versionMap;
    }
}
