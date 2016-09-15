package eu.openminted.registry.component.service;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import eu.openminted.interop.componentoverview.exporter.Exporter;
import eu.openminted.interop.componentoverview.exporter.OpenMinTeDExporter;
import eu.openminted.interop.componentoverview.importer.CreoleImporter;
import eu.openminted.interop.componentoverview.importer.Importer;
import eu.openminted.interop.componentoverview.importer.UimaImporter;
import eu.openminted.interop.componentoverview.model.ComponentMetaData;
import groovy.lang.Writable;

public class ComponentRegistryService {

	public List<Document> describe(String groupID, String artifactID, String version) throws IOException {
		Document description = createComponentXML();

		Artifact artifactObj = new DefaultArtifact(groupID, artifactID, "jar", version);

		// TODO this shouldn't be hardcoded
		RemoteRepository central = new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/")
				.build();

		ArtifactRequest artifactRequest = new ArtifactRequest();
		artifactRequest.setArtifact(artifactObj);
		artifactRequest.addRepository(central);

		try {
			ArtifactResult artifactResult = getRepositorySystem().resolveArtifact(getRepositorySession(),
					artifactRequest);

			return describe(artifactResult.getArtifact().getFile().toURI().toURL(), description);
		} catch (ArtifactResolutionException e) {
			throw new IOException("unable to retrieve plugin from maven", e);
		}
	}

	/**
	 * describes the components available in the jar at the given URL
	 */
	public List<Document> describe(URL jarURL) throws IOException {
		return describe(jarURL, createComponentXML());
	}

	/**
	 * describes the components available in the jar at the given URL mixing in
	 * any other information that may already have been provided
	 */
	public List<Document> describe(URL jarURL, Document openmintedComponentXML) throws IOException {

		// TODO extra information from the GATE/UIMA component at the URL (might
		// need to be in a loop if a single jar can define multiple components)
		List<Document> descriptions = new ArrayList<Document>();
		
		List<ComponentMetaData> metadata = null;

		// TODO how do we determine if a JAR is a GATE or UIMA (and which
		// framework) so we can use the correct importer to extract the relevant
		// metadata?

		URL baseURL = new URL("jar:" + jarURL + "!/");
		// System.out.println(baseURL);

		JarURLConnection connection = (JarURLConnection) baseURL.openConnection();

		Importer<ComponentMetaData> importer = null;
		
		JarFile jarFile = connection.getJarFile();
		ZipEntry entry = null;

		if (jarFile.getEntry("creole.xml") != null) {
			// if it has a creole.xml at the root then this is a GATE component
			importer = new CreoleImporter();

			URL directoryXmlFileUrl = new URL(baseURL, "creole.xml");
			metadata = importer.process(directoryXmlFileUrl);

		} else if ((entry = jarFile.getEntry("META-INF/org.apache.uima.fit/components.txt")) != null) {
			//WARNING, you are entering the mad world of UIMA, abandon all hope!
			
			List<String> lines = IOUtils.readLines(jarFile.getInputStream(entry));
			metadata = new ArrayList<ComponentMetaData>();
			for (String line : lines) {
				if (line.startsWith("classpath")) {
					line = line.split(":")[1];
					
					importer = new UimaImporter("uimaFIT");
					
					metadata.addAll(importer.process(new URL(baseURL,line)));
				}
			}
			
		}
		else {			
			throw new IOException("URL points to unknown component type:" + jarURL);
		}
		
		for (ComponentMetaData item : metadata) {
			//Document itemMetadata = (Document)openmintedComponentXML.clone();
			
			//TODO copy useful info from the found metadata into the base OpenMinTeD document
			
			//This should be an OpenMinTeD exporter rather than the metashare one
			Exporter<Writable> mse = new OpenMinTeDExporter();
			Writable w = mse.process(item);
			
			Document itemMetadata = null;
			
			try {
				itemMetadata = new SAXBuilder().build(new StringReader(w.toString()));			
				descriptions.add(itemMetadata);
			} catch (JDOMException e) {
				// this should be impossible
			}

		}

		return descriptions;
	}

	/**
	 * Register the component described by the XML document. This method is the
	 * one that actually registers the component with the metadata service all
	 * the other methods end up calling this one eventually
	 */
	public void register(Document openmintedComponentXML) {
		// TODO wrap the component XML and store it as a pending registration
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

	private static RepositorySystem repoSystem = null;

	private static DefaultRepositorySystemSession repoSystemSession = null;

	private static RepositorySystem getRepositorySystem() {
		if (repoSystem != null)
			return repoSystem;

		DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
		locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
		locator.addService(TransporterFactory.class, FileTransporterFactory.class);
		locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

		repoSystem = locator.getService(RepositorySystem.class);

		return repoSystem;
	}

	private static RepositorySystemSession getRepositorySession() {
		if (repoSystemSession != null)
			return repoSystemSession;

		repoSystemSession = MavenRepositorySystemUtils.newSession();

		// TODO pull this from the maven settings.xml file
		LocalRepository localRepo = new LocalRepository(
				System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository/");
		System.out.println(localRepo);
		repoSystemSession.setLocalRepositoryManager(
				getRepositorySystem().newLocalRepositoryManager(repoSystemSession, localRepo));

		return repoSystemSession;
	}
}
