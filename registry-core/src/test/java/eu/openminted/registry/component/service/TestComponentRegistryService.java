package eu.openminted.registry.component.service;

import eu.openminted.interop.componentoverview.model.ComponentMetaData;
import junit.framework.TestCase;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class TestComponentRegistryService extends TestCase {

	public void testGATEJar() throws IOException {
		ComponentRegistryService crs = new ComponentRegistryService();
		
		URL jarURL = this.getClass().getClassLoader().getResource("gate-component-test.jar");

		System.out.println("Making a call @ " + jarURL);
		List<ComponentMetaData> descriptions = crs.describe(jarURL);
		
		assertEquals(16, descriptions.size());
		
	}
	
	public void testUimaFITJar() throws IOException {
		ComponentRegistryService crs = new ComponentRegistryService();
		
		URL jarURL = this.getClass().getClassLoader().getResource("uimaFIT-component-test.jar");

		System.out.println("Making a call @ " + jarURL);
		List<ComponentMetaData> descriptions = crs.describe(jarURL);
		
		assertEquals(8, descriptions.size());
		
	}
	
	public void testUimaFITMaven() throws IOException {
		ComponentRegistryService crs = new ComponentRegistryService();

		List<ComponentMetaData> descriptions = crs.describe("de.tudarmstadt.ukp.dkpro.core","de.tudarmstadt.ukp.dkpro.core.stanfordnlp-gpl","1.8.0");
		
		assertEquals(8, descriptions.size());
		
	}
}
