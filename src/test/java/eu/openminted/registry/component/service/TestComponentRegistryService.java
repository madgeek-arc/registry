package eu.openminted.registry.component.service;

import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

public class TestComponentRegistryService extends TestCase {

	public void testGATEJar() throws IOException {
		ComponentRegistryService crs = new ComponentRegistryService();
		URL jarURL = null;
		crs.describe(jarURL);
		
	}
}
