package gr.uoa.di.madgik.registry.validation;

import org.w3c.dom.ls.LSInput;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Created by stefanos on 30/1/2017.
 */


public class SchemaInput implements LSInput {

    private String publicId;

    private String systemId;

    private String baseUri;
    private BufferedInputStream inputStream;

    public SchemaInput(String publicId, String sysId, InputStream input, String baseUri) {
        this.publicId = publicId;
        this.systemId = sysId;
        this.inputStream = new BufferedInputStream(input);
        this.baseUri = baseUri;
    }

    public String getPublicId() {
        return publicId;
    }

    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    public String getBaseURI() {
        return baseUri;
    }

    public void setBaseURI(String baseURI) {
        this.baseUri = baseURI;
    }

    public InputStream getByteStream() {
        return null;
    }

    public void setByteStream(InputStream byteStream) {
    }

    public boolean getCertifiedText() {
        return false;
    }

    public void setCertifiedText(boolean certifiedText) {
    }

    public Reader getCharacterStream() {
        return null;
    }

    public void setCharacterStream(Reader characterStream) {
    }

    public String getEncoding() {
        return null;
    }

    public void setEncoding(String encoding) {
    }

    public String getStringData() {
        synchronized (inputStream) {
            try {
                byte[] input = new byte[inputStream.available()];
                inputStream.read(input);
                String contents = new String(input);
                return contents;
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Exception " + e);
                return null;
            }
        }
    }

    public void setStringData(String stringData) {
    }

    public String getSystemId() {
        return systemId;
    }

    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public BufferedInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(BufferedInputStream inputStream) {
        this.inputStream = inputStream;
    }
}

