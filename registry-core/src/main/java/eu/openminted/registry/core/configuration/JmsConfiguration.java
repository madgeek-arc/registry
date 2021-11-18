package eu.openminted.registry.core.configuration;


import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
@PropertySource(value = {"classpath:application.properties", "classpath:registry.properties"})
public class JmsConfiguration {

    private static Logger logger = LogManager.getLogger(JmsConfiguration.class);

    @Value("${jms.host}")
    private String jmsHost;

    @Value("${jms.prefix:registry}")
    private String jmsPrefix;

    @Value("${jms.user:@null}")
    private String jmsUser;

    @Value("${jms.password:@null}")
    private String jmsPassword;

    @Bean
    public ActiveMQConnectionFactory activeMQConnectionFactory() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL(jmsHost);

        if (jmsUser != null)
            connectionFactory.setUserName(jmsUser);
        if (jmsPassword != null)
            connectionFactory.setPassword(jmsPassword);

        connectionFactory.setConnectionIDPrefix(jmsPrefix);
        logger.info("ActiveMQConnection Factory created for " + jmsHost);

        return connectionFactory;
    }

    @Bean // Serialize message content to json using TextMessage
    public MessageConverter jacksonJmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        return converter;
    }

    private DefaultJmsListenerContainerFactory getListenerContainerFactory(boolean b) {
        DefaultJmsListenerContainerFactory factory
                = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(activeMQConnectionFactory());
        factory.setPubSubDomain(b); // false is for queue
        factory.setMessageConverter(jacksonJmsMessageConverter());
        return factory;
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory() {
        return getListenerContainerFactory(false);
    }

    @Bean
    public DefaultJmsListenerContainerFactory jmsTopicListenerContainerFactory() {
        return getListenerContainerFactory(true);
    }

    private JmsTemplate getJmsTemplate(boolean b) {
        JmsTemplate template = new JmsTemplate();
        template.setConnectionFactory(activeMQConnectionFactory());
        template.setPubSubDomain(b); //false is for queue
        template.setMessageConverter(jacksonJmsMessageConverter());
        return template;
    }

    @Bean
    public JmsTemplate jmsQueueTemplate() {
        return getJmsTemplate(false);
    }

    @Bean
    public JmsTemplate jmsTopicTemplate() {
        return getJmsTemplate(true);
    }

    public String getJmsPrefix() {
        return jmsPrefix;
    }
}
