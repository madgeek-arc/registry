package eu.openminted.registry.core.configuration;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by stefanos on 14/11/2016.
 */
@Configuration
@ComponentScan({ "eu.openminted.registry.core.elasticsearch" })
@PropertySource(value = { "classpath:application.properties", "classpath:registry.properties"} )
public class ElasticConfiguration {

    @Autowired
    private Environment environment;

    @Value("${elasticsearch.port}")
    String port;

    @Value("${elasticsearch.url}")
    String hostname;

//    @Value("${elasticsearch.cluster}")
//    String clusterName;

    @Bean
    public Client client(){
        TransportClient client = null;
        Settings.Builder settings = Settings.builder();

        //check if part of a cluster and add it
        if(environment.getProperty("elasticsearch.cluster","") != "") {
            settings.put("cluster.name", environment.getRequiredProperty("elasticsearch.cluster"));
        }

        try {
            client = new PreBuiltTransportClient(settings.build())
                    .addTransportAddress(new InetSocketTransportAddress(
                                    InetAddress.getByName(
                                            environment.getRequiredProperty("elasticsearch.url")),
                                    Integer.parseInt(environment.getRequiredProperty("elasticsearch.port"))
                            )
                    );
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return client;
    }
}
