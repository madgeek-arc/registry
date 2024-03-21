package eu.openminted.registry.core.configuration;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Creates RestHighLevelClient Bean
 */
@Configuration
@EnableRetry
public class ElasticConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

    private final Environment environment;
    private final String port;
    private final String hostname;
    private final String username;
    private final String password;
    private final String scheme;
    private RestHighLevelClient client = null;

    public ElasticConfiguration(Environment environment) {
        this.hostname = environment.getRequiredProperty("elasticsearch.url");
        this.port = environment.getRequiredProperty("elasticsearch.port");
        this.username = environment.getProperty("elasticsearch.username", "");
        this.password = environment.getProperty("elasticsearch.password", "");
        this.scheme = environment.getProperty("elasticsearch.scheme", "http");
        this.environment = environment;
    }

    @PostConstruct
    private void init() {
        Settings.Builder settings = Settings.builder();

        //check if part of a cluster and add it
        if (!"".equals(environment.getProperty("elasticsearch.cluster", ""))) {
            settings.put("cluster.name", environment.getRequiredProperty("elasticsearch.cluster"));
        }

        logger.info("Connecting to Elasticsearch @ {}:{} as user {}", hostname, port, username);

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(hostname, Integer.parseInt(port), scheme))
                .setHttpClientConfigCallback(httpClientBuilder ->
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        ;

        this.client = new RestHighLevelClient(restClientBuilder);
    }

    @PreDestroy
    private void destroy() throws IOException {
        this.client.close();
    }

    @Bean
    public RestHighLevelClient getClient() {
        return this.client;
    }
}
