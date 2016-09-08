package eu.openminted.registry.component.service;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.jdom.Document;

import junit.framework.TestCase;

public class TestComponentRegistryService extends TestCase {

	public void testGATEJar() throws IOException {
		ComponentRegistryService crs = new ComponentRegistryService();
		
		URL jarURL = this.getClass().getClassLoader().getResource("gate-component-test.jar");
		
		List<Document> descriptions = crs.describe(jarURL);
		
		assertEquals(16, descriptions.size());
		
	}
}
