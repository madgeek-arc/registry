package eu.openminted.registry.core.jms.service;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.naming.InitialContext;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("jmsService")
public class JMSService {

    private TopicConnection connection = null;
    private TopicSession session = null;
    private Topic topic = null;
	private InitialContext iniCtx;
    
	@Autowired
	private Environment environment;
	
	public JMSService(){
		
	}
	
	public String createTopicFunction(String topicName){
		try {
	
			TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(environment.getProperty("jms.host"));
			connection = connectionFactory.createTopicConnection();
			session = connection.createTopicSession(false,Session.AUTO_ACKNOWLEDGE);
					
			connection.start();	
			Topic topic = session.createTopic(topicName);
			session.close();
			if (connection != null) {
				connection.close();
			}
			return "All good";
		} catch (JMSException e) {
			return e.getMessage();
		} catch (Exception e) {
			return e.getMessage();
		}
		
	}
	
	public String publishMessage(String topicName, String message){
		
		 try {
			 TopicConnectionFactory connectionFactory = new ActiveMQConnectionFactory(environment.getProperty("jms.host"));
			 connection = connectionFactory.createTopicConnection();
			 session = connection.createTopicSession(false,Session.AUTO_ACKNOWLEDGE);
						
			 connection.start();	
			 Topic topic = session.createTopic(topicName);
			 
		     TopicPublisher send = session.createPublisher(topic);
		     TextMessage tm = session.createTextMessage(message);
		     send.publish(tm);
		     send.close();
		     return "All good";
		}  catch (JMSException e) {
			return e.getMessage();
		}
		
	}
	
	public void closeConnection(){
		try {
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
}
