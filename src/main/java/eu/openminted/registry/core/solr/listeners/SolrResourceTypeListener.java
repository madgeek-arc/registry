package eu.openminted.registry.core.solr.listeners;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import eu.openminted.registry.core.domain.ResourceType;
import eu.openminted.registry.core.monitor.ResourceTypeListener;
import eu.openminted.registry.core.solr.service.SolrOperationsService;

@Component
public class SolrResourceTypeListener implements ResourceTypeListener{

	private Logger logger = Logger.getLogger(SolrResourceTypeListener.class);

	@Autowired
	SolrOperationsService solrOperationService;
	
	@Override
	public void resourceTypeAdded(ResourceType resourceType) {
		try {
			solrOperationService.createCore(resourceType);
		} catch (Exception e) {
			logger.error("Error creating core", e);
		}
	}

}
