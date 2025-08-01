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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import gr.uoa.di.madgik.registry_starter.jms.JmsResourceListener;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageType;

import java.text.SimpleDateFormat;

@AutoConfiguration
@ConditionalOnClass(JmsProperties.class)
@ConditionalOnProperty(prefix = "registry.jms", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JmsProperties.class)
@EnableJms
public class JmsAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(JmsAutoConfiguration.class);

    @Configuration(proxyBeanMethods = false)
    @Import(JmsResourceListener.class)
    public static class ConfigureJms {

        public ConfigureJms() {
            logger.info("Jms Autoconfiguration enabled");
        }

        @Bean
        @ConditionalOnMissingBean
        public ActiveMQConnectionFactory activeMQConnectionFactory(JmsProperties jmsProperties) {
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
            connectionFactory.setBrokerURL(jmsProperties.getHost());
            connectionFactory.setUserName(jmsProperties.getUsername());
            connectionFactory.setPassword(jmsProperties.getPassword());
            connectionFactory.setConnectionIDPrefix(jmsProperties.getPrefix());
            logger.info("ActiveMQConnection Factory created for {}", jmsProperties.getHost());

            return connectionFactory;
        }

        @Bean // override registry bean
        public MappingJackson2MessageConverter jacksonJmsMessageConverter() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));

            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setTargetType(MessageType.TEXT);
            converter.setTypeIdPropertyName("_type");
            converter.setObjectMapper(objectMapper);
            return converter;
        }

//        @Bean
//        @ConditionalOnBean(value = ActiveMQConnectionFactory.class, name = "jacksonJmsMessageConverter")
//        public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory connectionFactory,
//                                                                                   MappingJackson2MessageConverter jacksonJmsMessageConverter) {
//            DefaultJmsListenerContainerFactory factory
//                    = new DefaultJmsListenerContainerFactory();
//            factory.setConnectionFactory(connectionFactory);
//            factory.setPubSubDomain(false); // false is for queue
//            factory.setMessageConverter(jacksonJmsMessageConverter);
//            return factory;
//        }
//
//        @Bean
//        @ConditionalOnBean(name = "jacksonJmsMessageConverter")
//        public DefaultJmsListenerContainerFactory jmsTopicListenerContainerFactory(ActiveMQConnectionFactory connectionFactory,
//                                                                                   MappingJackson2MessageConverter jacksonJmsMessageConverter) {
//            DefaultJmsListenerContainerFactory factory
//                    = new DefaultJmsListenerContainerFactory();
//            factory.setConnectionFactory(connectionFactory);
//            factory.setPubSubDomain(true); // true is for topic
//            factory.setMessageConverter(jacksonJmsMessageConverter);
//            return factory;
//        }

        @Bean
        @ConditionalOnBean(value = {ActiveMQConnectionFactory.class, MappingJackson2MessageConverter.class})
        public JmsTemplate jmsQueueTemplate(ActiveMQConnectionFactory connectionFactory,
                                            MappingJackson2MessageConverter jacksonJmsMessageConverter) {
            JmsTemplate template = new JmsTemplate();
            template.setConnectionFactory(connectionFactory);
            template.setPubSubDomain(false); //false is for queue
            template.setMessageConverter(jacksonJmsMessageConverter);
            return template;
        }

        @Bean
        @ConditionalOnBean(value = {ActiveMQConnectionFactory.class, MappingJackson2MessageConverter.class})
        public JmsTemplate jmsTopicTemplate(ActiveMQConnectionFactory connectionFactory,
                                            MappingJackson2MessageConverter jacksonJmsMessageConverter) {
            JmsTemplate template = new JmsTemplate();
            template.setConnectionFactory(connectionFactory);
            template.setPubSubDomain(true); //true is for topic
            template.setMessageConverter(jacksonJmsMessageConverter);
            return template;
        }
    }
}
