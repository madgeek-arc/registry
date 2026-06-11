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

import tools.jackson.databind.module.SimpleModule;

import java.time.Instant;

/**
 * Jackson module that registers {@link FlexibleInstantDeserializer} for {@link Instant}.
 * Registered via {@code META-INF/services/tools.jackson.databind.JacksonModule} so that
 * any {@code ObjectMapper} built with {@code findAndAddModules()} picks it up automatically,
 * including mappers defined in downstream projects.
 */
public class FlexibleInstantModule extends SimpleModule {

    public FlexibleInstantModule() {
        super("flexible-instant");
        addDeserializer(Instant.class, new FlexibleInstantDeserializer());
    }
}
