package gr.uoa.di.madgik.registry.exception;

/**
 * Created by stefanos on 15-Nov-16.
 */
@Deprecated
public class ServerError {

    public final String url;
    public final String error;

    public ServerError(String url, Exception ex) {
        this.url = url;
        this.error = ex.getMessage();
    }

    public String getUrl() {
        return url;
    }

    public String getError() {
        return error;
    }
}
