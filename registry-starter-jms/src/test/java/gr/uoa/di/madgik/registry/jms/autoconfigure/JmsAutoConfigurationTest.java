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

package gr.uoa.di.madgik.registry.jms.autoconfigure;

import gr.uoa.di.madgik.registry.jms.JmsResourceListener;
import gr.uoa.di.madgik.registry.monitor.ResourceListener;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jms.core.JmsTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class JmsAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JmsAutoConfiguration.class));

    @Test
    void registersBrokerConnectionTopicTemplateAndListenerByDefault() {
        contextRunner
                .withPropertyValues(
                        "registry.jms.host=tcp://broker.example.org:61616",
                        "registry.jms.username=user",
                        "registry.jms.password=secret",
                        "registry.jms.prefix=test")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JmsResourceListener.class);
                    assertThat(context).getBeans(ResourceListener.class).hasSize(1);

                    ActiveMQConnectionFactory factory = context.getBean(ActiveMQConnectionFactory.class);
                    assertThat(factory.getBrokerURL()).isEqualTo("tcp://broker.example.org:61616");
                    assertThat(factory.getUserName()).isEqualTo("user");
                    assertThat(factory.getPassword()).isEqualTo("secret");

                    JmsTemplate template = context.getBean("jmsTopicTemplate", JmsTemplate.class);
                    assertThat(template.isPubSubDomain()).isTrue();
                });
    }

    @Test
    void backsOffWhenDisabled() {
        contextRunner
                .withPropertyValues("registry.jms.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(JmsResourceListener.class);
                    assertThat(context).doesNotHaveBean(ActiveMQConnectionFactory.class);
                });
    }

    @Test
    void usesApplicationConnectionFactoryWhenDefined() {
        ActiveMQConnectionFactory custom = new ActiveMQConnectionFactory("vm://custom");
        contextRunner
                .withBean(ActiveMQConnectionFactory.class, () -> custom)
                .run(context -> assertThat(context.getBean(ActiveMQConnectionFactory.class)).isSameAs(custom));
    }

    @Test
    void nestedConfigurationPickedUpByComponentScanAlsoBacksOffWhenDisabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(JmsAutoConfiguration.ConfigureJms.class)
                .withPropertyValues("registry.jms.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(JmsResourceListener.class);
                });
    }
}
