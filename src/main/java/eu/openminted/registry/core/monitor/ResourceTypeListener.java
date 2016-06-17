package eu.openminted.registry.core.monitor;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.solr.client.solrj.SolrServerException;
import org.xml.sax.SAXException;

import eu.openminted.registry.core.domain.ResourceType;

/**
 * Created by antleb on 5/30/16.
 */
public interface ResourceTypeListener {

	public void resourceTypeAdded(ResourceType resourceType) throws IOException, URISyntaxException, SolrServerException, ParserConfigurationException, SAXException, TransformerException, InterruptedException;
}
