package gr.uoa.di.madgik.registry_starter.autoconfigure;


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

        @Bean // Serialize message content to json using TextMessage
        public MappingJackson2MessageConverter jacksonJmsMessageConverter() {
            MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
            converter.setTargetType(MessageType.TEXT);
            converter.setTypeIdPropertyName("_type");
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
