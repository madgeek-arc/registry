package eu.openminted.registry.component.service;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;

public class ComponentRegistryService {

	public List<Document> describe(String groupID, String artifactID, String version) {
		Document description = createComponentXML();
		
		//TODO get hold of the jar and pass that on
		
		return describe(null, description);
	}
	
	
	/**
	 * Registers a jar available via the URL as a component
	 */
	public List<Document> describe(URL jarURL) {
		return describe(jarURL, createComponentXML());
	}

	/**
	 * Registers the component along with any other available information
	 */
	public List<Document> describe(URL jarURL, Document openmintedComponentXML) {
		
		// TODO extra information from the GATE/UIMA component at the URL (might
		// need to be in a loop if a single jar can define multiple components)
		List<Document> descriptions = new ArrayList<Document>();
		
		return descriptions;		
	}

	/**
	 * Register the component described by the XML document. This method is the
	 * one that actually registers the component with the metadata service all
	 * the other methods end up calling this one eventually
	 */
	public void register(Document openmintedComponentXML) {
		//TODO wrap the component XML and store it as a pending registration
	}

	public Document createComponentXML() {
		Document doc = new Document();

		// TODO add the basic elements to the document before returning it

		return doc;
	}

	/**
	 * @return true if the component has been registered with the system even if
	 *         registration is still pending, false otherwise
	 */
	public boolean isKnown(String className, String version) {
		return isRegistered(className, version) || isPending(className, version);
	}

	/**
	 * @return true if this component is in the pending queue prior to full
	 *         registration, false otherwise
	 */
	public boolean isPending(String className, String version) {
		// TODO implement the lookup within the metadata service
		return false;
	}

	/**
	 * @return true if this component is fully registered and available for use,
	 *         false otherwise
	 */
	public boolean isRegistered(String className, String version) {
		// TODO implement the lookup within the metadata service
		return false;
	}
}
