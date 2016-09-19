package eu.openminted.registry.solr;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;

/**
 * Created by antleb on 18/9/2016.
 */
public class TestCores {

	private static SolrClient solrClient;

	private String coreName;

	@BeforeClass
	public static void init() {
		solrClient = new HttpSolrClient("http://localhost:8983/solr");
	}

	@Before
	public void beforeTest() {
		coreName = "testCore-" + new Date().getTime();
	}

	@After
	public void afterTest() throws IOException, SolrServerException {
		CoreAdminRequest.Unload unloadCore = new CoreAdminRequest.Unload(true);

		unloadCore.setCoreName(coreName);
		unloadCore.setDeleteDataDir(true);
		unloadCore.setDeleteIndex(true);
		unloadCore.setDeleteInstanceDir(true);

		solrClient.request(unloadCore);
	}

	//@Test
	public void testBasicCoreCreation() throws IOException, SolrServerException {
		createCore();
	}

	private void createCore() throws SolrServerException, IOException {
		CoreAdminRequest.Create createCore = new CoreAdminRequest.Create();
		createCore.setCoreName(coreName);

		createCore.setConfigSet("omtd_registry");

		solrClient.request(createCore);
	}
}
