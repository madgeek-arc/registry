package eu.openminted.registry.core.jms.service;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.jms.*;

@Service("jmsService")
public class JMSService {

	private TopicConnection connection = null;
	private TopicSession session = null;

	@Autowired
	private Environment environment;

	public JMSService() {

	}

	public void createTopic(String topicName) throws JMSException {
		TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(environment.getProperty("jms.host"));
		connection = connectionFactory.createTopicConnection();
		session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		connection.start();
		session.createTopic(topicName);
		session.close();

		if (connection != null) {
			connection.close();
		}
	}

	public void publishMessage(String topicName, String message) throws JMSException {

		TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(environment.getProperty("jms.host"));
		connection = connectionFactory.createTopicConnection();
		session = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		connection.start();
		Topic topic = session.createTopic(topicName);

		TopicPublisher send = session.createPublisher(topic);
		TextMessage tm = session.createTextMessage(message);
		send.publish(tm);
		send.close();

	}
}
