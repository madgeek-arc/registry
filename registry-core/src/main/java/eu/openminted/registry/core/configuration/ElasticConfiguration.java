package eu.openminted.registry.core.configuration;

import org.apache.http.HttpHost;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.context.annotation.RequestScope;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Created by stefanos on 14/11/2016.
 */
@Configuration
@EnableRetry
@ComponentScan({ "eu.openminted.registry.core.elasticsearch" })
@PropertySource(value = { "classpath:application.properties", "classpath:registry.properties"} )
public class ElasticConfiguration {


    private static Logger logger = LogManager.getLogger(ElasticConfiguration.class);

    @Autowired
    private Environment environment;

    @Value("${elasticsearch.port}")
    String port;

    @Value("${elasticsearch.url}")
    String hostname;

    private RestHighLevelClient client = null;

    @PostConstruct
    private void init() {
        Settings.Builder settings = Settings.builder();

        //check if part of a cluster and add it
        if(environment.getProperty("elasticsearch.cluster","") != "") {
            settings.put("cluster.name", environment.getRequiredProperty("elasticsearch.cluster"));
        }

        logger.info("Connecting to Elasticsearch @ "+hostname+":"+port);
        RestClientBuilder restClientBuilder =  RestClient.builder(
                new HttpHost(hostname, Integer.parseInt(port), "http")).setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder.setConnectTimeout(10000).setSocketTimeout(30000));

        this.client = new RestHighLevelClient(restClientBuilder);
    }

    @PreDestroy
    private void destroy() throws IOException {
        this.client.close();
    }

    @Bean
    @Scope("prototype")
    public RestHighLevelClient getClient() {
        return this.client;
    }
}
