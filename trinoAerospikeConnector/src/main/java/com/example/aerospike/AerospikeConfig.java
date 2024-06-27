package com.example.aerospike;
import java.util.Map;
public class AerospikeConfig {
    private final String host;
    private final int port;
    private final String namespace;

    public AerospikeConfig(Map<String, String> config) {
        this.host = config.getOrDefault("aerospike.host", "localhost");
        this.port = Integer.parseInt(config.getOrDefault("aerospike.port", "3000"));
        this.namespace = config.getOrDefault("aerospike.namespace", "test");
        // Add other configuration parameters as needed
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getNamespace() {
        return namespace;
    }

}
