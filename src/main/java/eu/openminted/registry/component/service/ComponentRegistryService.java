package eu.openminted.registry.component.service;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

public class ComponentRegistryService {

	public List<Document> describe(String groupID, String artifactID,
			String version) throws IOException {
		Document description = createComponentXML();

		Artifact artifactObj = new DefaultArtifact(groupID, artifactID, "jar",
				version);

		//TODO this shouldn't be hardcoded
		RemoteRepository central = new RemoteRepository.Builder("central",
				"default", "http://repo1.maven.org/maven2/").build();

		ArtifactRequest artifactRequest = new ArtifactRequest();
		artifactRequest.setArtifact(artifactObj);
		artifactRequest.addRepository(central);

		try {
			ArtifactResult artifactResult = getRepositorySystem()
					.resolveArtifact(getRepositorySession(), artifactRequest);

			return describe(artifactResult.getArtifact().getFile().toURI()
					.toURL(), description);
		} catch (ArtifactResolutionException e) {
			throw new IOException("unable to retrieve plugin from maven", e);
		}
	}
	
	
	/**
	 * Registers a jar available via the URL as a component
	 */
	public List<Document> describe(URL jarURL) throws IOException {
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
	
	private static RepositorySystem repoSystem = null;

    private static DefaultRepositorySystemSession repoSystemSession = null;

    private static RepositorySystem getRepositorySystem() {
      if(repoSystem != null) return repoSystem;

      DefaultServiceLocator locator =
              MavenRepositorySystemUtils.newServiceLocator();
      locator.addService(RepositoryConnectorFactory.class,
              BasicRepositoryConnectorFactory.class);
      locator.addService(TransporterFactory.class, FileTransporterFactory.class);
      locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

      repoSystem = locator.getService(RepositorySystem.class);

      return repoSystem;
    }

    private static RepositorySystemSession getRepositorySession() {
      if(repoSystemSession != null) return repoSystemSession;

      repoSystemSession = MavenRepositorySystemUtils.newSession();

      //TODO pull this from the maven settings.xml file
      LocalRepository localRepo =
              new LocalRepository(System.getProperty("user.home")+File.separator+".m2"+File.separator+"repository/");
      System.out.println(localRepo);
      repoSystemSession.setLocalRepositoryManager(getRepositorySystem()
              .newLocalRepositoryManager(repoSystemSession, localRepo));

      return repoSystemSession;
    }
}
