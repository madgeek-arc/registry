package eu.openminted.registry.core.jms.listeners;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.jms.service.JMSService;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import net.minidev.json.JSONObject;

public class JmsResourceTypeListener implements ResourceTypeListener{

	@Autowired
	JMSService jmsService;
	
	@Override
	public void resourceTypeAdded(ResourceType resourceType)
			throws IOException, URISyntaxException, SolrServerException, ParserConfigurationException, SAXException,
			TransformerException, InterruptedException {
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("message", "A new resource with name:"+resourceType.getName()+" has been added");
		jmsService.publishMessage("General", jsonObject.toString());
		jmsService.createTopicFunction(resourceType.getName()+"-create");
		jmsService.createTopicFunction(resourceType.getName()+"-update");
		jmsService.createTopicFunction(resourceType.getName()+"-delete");
		jmsService.closeConnection();
	}

}
