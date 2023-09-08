package eu.openminted.registry.core.configuration;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Created by stefanos on 14/11/2016.
 */
@Configuration
@EnableRetry
public class ElasticConfiguration {


    private static final Logger logger = LoggerFactory.getLogger(ElasticConfiguration.class);

    private final Environment environment;
    private final String port;
    private final String hostname;

    @Autowired
    public ElasticConfiguration(Environment environment) {
        this.hostname = environment.getRequiredProperty("elasticsearch.url");
        this.port = environment.getRequiredProperty("elasticsearch.port");
        this.environment = environment;
    }

    private RestHighLevelClient client = null;

    @PostConstruct
    private void init() {
        Settings.Builder settings = Settings.builder();

        //check if part of a cluster and add it
        if (!"".equals(environment.getProperty("elasticsearch.cluster", ""))) {
            settings.put("cluster.name", environment.getRequiredProperty("elasticsearch.cluster"));
        }

        logger.info("Connecting to Elasticsearch @ {}:{}", hostname, port);
        RestClientBuilder restClientBuilder = RestClient.builder(new HttpHost(hostname, Integer.parseInt(port), "http"));
//                .setRequestConfigCallback(requestConfigBuilder ->
//                requestConfigBuilder.setConnectTimeout(10000).setSocketTimeout(30000));

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
