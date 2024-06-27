package com.example.flow;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.policy.QueryPolicy;
import com.aerospike.client.query.Statement;
import io.prestosql.metadata.Metadata;
import io.prestosql.metadata.Split;

import javax.xml.crypto.Data;

public class AerospikeConnector {

    public Metadata fetchMetadata() {
        // Fetch metadata from Aerospike
        // Example: connect to Aerospike using client and retrieve metadata
        AerospikeClient client = new AerospikeClient("localhost", 3000);
        Metadata metadata = null;//client.getMetadata();
        return metadata;
    }

    public Data fetchRecords(Split split) {
        // Fetch records from Aerospike based on split
        // Example: query Aerospike based on split information
        AerospikeClient client = new AerospikeClient("localhost", 3000);
        Data records = (Data) client.query(new QueryPolicy(),new Statement());
        return records;
    }
}
