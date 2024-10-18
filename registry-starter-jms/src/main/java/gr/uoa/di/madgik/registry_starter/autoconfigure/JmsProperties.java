package gr.uoa.di.madgik.registry_starter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "registry.jms")
public class JmsProperties {

    /**
     * Enable JMS Messages on resource operations.
     */
    private boolean enabled = true;

    /**
     * JMS host. Should contain transport scheme and port. Default= tcp://localhost:61616
     */
    private String host = "tcp://localhost:61616";

    /**
     * Queue/Topic prefix to use for resource operation messages.
     */
    private String prefix = "registry";

    /**
     * Username to connect to JMS
     */
    private String username = "";

    /**
     * Password to connect to JMS
     */
    private String password = "";

    public JmsProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
