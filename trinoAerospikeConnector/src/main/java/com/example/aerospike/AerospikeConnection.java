package com.example.aerospike;
import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.ClientPolicy;
public class AerospikeConnection {
    private final AerospikeClient client;

    public AerospikeConnection(AerospikeConfig config) {
        ClientPolicy policy = new ClientPolicy();
        this.client = new AerospikeClient(policy, config.getHost(), config.getPort());
    }

    public AerospikeClient getClient() {
        return client;
    }

    public void close() {
        client.close();
    }
}
