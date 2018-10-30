/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree
 */
package eu.openminted.registry.core.resourcesync.domain;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Richard Jones
 */
public class ResourceSyncLn
{
	
	
    protected Map<String, String> hashes = new HashMap<String, String>();
    protected String href = null;
    protected long length = -1;
    protected Date modified = null;
    protected String path = null;
    protected String rel = null;
    protected int pri = -1;
    protected String type = null;
    protected String encoding = null;

    public void addHash(String type, String hex)
    {
        this.hashes.put(type, hex);
    }

    public void calculateHash(String type, InputStream stream)
    {
        // TODO: could do this when we're a bit further down the development path
    }

    public Map<String, String> getHashes()
    {
        return this.hashes;
    }

    public String getHref()
    {
        return href;
    }

    public void setHref(String href)
    {
        this.href = href;
    }

    public long getLength()
    {
        return length;
    }

    public void setLength(long length)
    {
        this.length = length;
    }

    public Date getModified()
    {
        return modified;
    }

    public void setModified(Date modified)
    {
        this.modified = modified;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getRel()
    {
        return rel;
    }

    public void setRel(String rel)
    {
        this.rel = rel;
    }

    public int getPri()
    {
        return pri;
    }

    public void setPri(int pri)
    {
        this.pri = pri;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
    }
}
