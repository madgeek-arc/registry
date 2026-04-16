/*
 * Copyright 2018-2026 OpenAIRE AMKE & Athena Research and Innovation Center
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

package gr.uoa.di.madgik.registry.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@Configuration
@Import({
        HibernateConfiguration.class,
        BatchConfig.class,
        BackupRestoreConfig.class,
        ServiceConfiguration.class
})
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "gr.uoa.di.madgik.registry.dao",
        "gr.uoa.di.madgik.registry.repository",
        "gr.uoa.di.madgik.registry.service",
        "gr.uoa.di.madgik.registry.index",
        "gr.uoa.di.madgik.registry.monitor",
        "gr.uoa.di.madgik.registry.validation",
        "gr.uoa.di.madgik.registry.backup"
})

public class DatabaseConfiguration {

    public static final String TEST_RESOURCE_ID = "e98db949-f3e3-4d30-9894-7dd2e291fbef";
    public static final String TEST_MISSING_RESOURCE_ID = "not-existing-resource-id";
}
