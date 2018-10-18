package eu.openminted.registry.component.service;

import eu.openminted.interop.componentoverview.exporter.Exporter;
import eu.openminted.interop.componentoverview.exporter.OpenMinTeDExporter;
import eu.openminted.interop.componentoverview.importer.CreoleImporter;
import eu.openminted.interop.componentoverview.importer.Importer;
import eu.openminted.interop.componentoverview.importer.UimaImporter;
import eu.openminted.interop.componentoverview.model.ComponentMetaData;
import groovy.util.Node;
import groovy.xml.XmlUtil;
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
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class ComponentRegistryService {

	public List<ComponentMetaData> describe(String groupID, String artifactID, String version) throws IOException {
		ComponentMetaData metadata = new ComponentMetaData();

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

			return describe(artifactResult.getArtifact().getFile().toURI().toURL(), metadata);
		} catch (ArtifactResolutionException e) {
			throw new IOException("unable to retrieve plugin from maven", e);
		}
	}

	/**
	 * describes the components available in the jar at the given URL
	 */
	public List<ComponentMetaData> describe(URL jarURL) throws IOException {
		return describe(jarURL, new ComponentMetaData());
	}

	/**
	 * describes the components available in the jar at the given URL mixing in
	 * any other information that may already have been provided
	 */
	public List<ComponentMetaData> describe(URL jarURL, ComponentMetaData existingMetadata) throws IOException {

		List<ComponentMetaData> metadata = null;

		// TODO how do we determine if a JAR is a GATE or UIMA (and which
		// framework) so we can use the correct importer to extract the relevant
		// metadata?

		URL baseURL = new URL("jar:" + jarURL + "!/");

		JarURLConnection connection = (JarURLConnection) baseURL.openConnection();

		Importer<ComponentMetaData> importer = null;

		JarFile jarFile = connection.getJarFile();
		ZipEntry entry = null;

		if (jarFile.getEntry("creole.xml") != null) {
			// if it has a creole.xml at the root then this is a GATE component
			importer = new CreoleImporter();

			URL directoryXmlFileUrl = new URL(baseURL, "creole.xml");
			metadata = importer.process(directoryXmlFileUrl, existingMetadata);

		} else if ((entry = jarFile.getEntry("META-INF/org.apache.uima.fit/components.txt")) != null) {
			// WARNING, you are entering the mad world of UIMA, abandon all
			// hope!

			List<String> lines = IOUtils.readLines(jarFile.getInputStream(entry));
			metadata = new ArrayList<ComponentMetaData>();
			for (String line : lines) {
				if (line.startsWith("classpath")) {
					line = line.split(":")[1];

					importer = new UimaImporter("uimaFIT");

					metadata.addAll(importer.process(new URL(baseURL, line), existingMetadata));
				}
			}

		} else {
			throw new IOException("URL points to unknown component type:" + jarURL);
		}

		return metadata;
	}

	/**
	 * Register the component described by the XML document. This method is the
	 * one that actually registers the component with the metadata service all
	 * the other methods end up calling this one eventually
	 */
	public void register(ComponentMetaData metadata) {

		Document itemMetadata = null;
		Exporter<Node> mse = new OpenMinTeDExporter();
		Node w = mse.process(metadata);

		try {
			itemMetadata = new SAXBuilder().build(new StringReader(XmlUtil.serialize(w)));

			// TODO save the XML into the actual registry store

		} catch (IOException | JDOMException e) {
			// this should be impossible
		}
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
		repoSystemSession.setLocalRepositoryManager(
				getRepositorySystem().newLocalRepositoryManager(repoSystemSession, localRepo));

		return repoSystemSession;
	}
}
