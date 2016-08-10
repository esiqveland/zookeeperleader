package no.bekk.distsys.leader;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.smoketurner.dropwizard.consul.ConsulFactory;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.constraints.NotNull;

public class LeadingConfiguration extends Configuration {

    @JsonProperty
    @NotNull
    private String pingerHost;

    @JsonProperty
    private HttpClientConfiguration httpClient;
//    private String zooKeeper = "172.17.0.2:2181";
//    private String zooKeeper = "192.168.99.100:2181";
    private String zooKeeper = "localhost:2181";


    private ConsulFactory consul;

    public String getPingerHost() {
        return pingerHost;
    }

    public HttpClientConfiguration getHttpClient() {
        return httpClient;
    }

    public String getZooKeeper() {
        return zooKeeper;
    }

    public ConsulFactory getConsul() {
        return consul;
    }

    public void setConsul(ConsulFactory consul) {
        this.consul = consul;
    }
}
