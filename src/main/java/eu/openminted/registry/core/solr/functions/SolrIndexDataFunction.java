package eu.openminted.registry.core.solr.functions;

import java.io.IOException;

import javax.persistence.PersistenceException;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.domain.index.IndexedField;
import eu.openminted.registry.core.solr.service.SolrOperationsService;

public class SolrIndexDataFunction {
	private static Logger logger = Logger.getLogger(SolrIndexDataFunction.class);
	
	public void add(Resource resource){
		String resourceType = resource.getResourceType();
		SolrClient solrClient = SolrOperationsService.getSolrClient(resourceType);

		SolrInputDocument document = new SolrInputDocument();
		document.addField("id", resource.getId());
		document.addField("resourceType", resource.getResourceType());
		document.addField("payload", resource.getPayload());
		document.addField("version", resource.getVersion());
		document.addField("creation_date", resource.getCreationDate());
		document.addField("modification_date", resource.getModificationDate());
		for (IndexedField indexedField : resource.getIndexedFields()){
			document.addField(indexedField.getName(), indexedField.getValues());
		}
		
		try { 
            UpdateResponse response = solrClient.add(document);
            logger.debug("UpdateResponse from add of SolrInputDocument:  " + response);
        } catch (SolrServerException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "SolrServerException while adding Solr index for resource type " + resourceType, e); 
        } catch (IOException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "IOException while adding Solr index for resource type " + resourceType, e); 
        } catch (RuntimeException e) { 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "RuntimeException while adding Solr index for resource type " + resourceType, e); 
        } 
	}
	
	public void update(Resource resource){
		add(resource);
	}
	
	public void delete(Resource resource) throws PersistenceException { 
		String resourceType = resource.getResourceType();
		SolrClient solrClient = SolrOperationsService.getSolrClient(resourceType);
        if (solrClient == null) { 
            return; 
        }  
 
        try { 
        	logger.info("Deleting item with ID" + resource.getId()); 
        	solrClient.deleteById(resource.getId()); 
        } catch (SolrServerException e) { 
        	logger.info( 
                    "SolrServerException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "SolrServerException while trying to delete resource by ID for resource type " 
                            + resourceType, e); 
        } catch (IOException e) { 
        	logger.info("IOException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "IOException while trying to delete items by ID for persistent type " 
                            + resourceType, e); 
        } catch (RuntimeException e) { 
        	logger.info( 
                    "RuntimeException while trying to delete resource by ID for resource type " + resourceType, e); 
            doRollback(solrClient, resourceType); 
            throw new PersistenceException( 
                    "RuntimeException while trying to delete resource by ID for resource type " 
                            + resourceType, e); 
        }	
	}
	
	private void doRollback(SolrClient solrClient, String type) { 
        logger.debug("ENTERING: doRollback()"); 
        try { 
        	solrClient.rollback(); 
        } catch (SolrServerException e) { 
        	logger.info("SolrServerException while doing rollback for resource type " + type, e); 
        } catch (IOException e) { 
        	logger.info("IOException while doing rollback for resource type " + type, e); 
        } 
        logger.debug("EXITING: doRollback()"); 
    } 
	
}
