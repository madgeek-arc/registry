/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package eu.openminted.registry.core.resourcesync.domain;

/**
 * @author Richard Jones
 */
public class ResourceSyncDescriptionIndex extends SitemapIndex {

    public ResourceSyncDescriptionIndex(String capability) {
        super(capability);
    }

    public void addSourceDescription(Sitemap sourceDesc) {
        if (!ResourceSync.CAPABILITY_RESOURCESYNC.equals(sourceDesc.getCapability()))
//        {
//            throw new SpecComplianceException("URL added to ResourceSyncDescription is not a Capability List");
//        }
            this.addSitemap(sourceDesc);
    }

    public Sitemap addSourceDescription(String loc) {
        return this.addSourceDescription(loc, null);
    }

    public Sitemap addSourceDescription(String loc, String describedby) {
        Sitemap sourceDesc = new Sitemap();
        sourceDesc.setLoc(loc);
        if (describedby != null) {
            sourceDesc.addLn(ResourceSync.REL_DESCRIBED_BY, describedby);
        }
//        sourceDesc.setCapability(ResourceSync.CAPABILITY_RESOURCESYNC);
        this.addSourceDescription(sourceDesc);
        return sourceDesc;
    }


}
