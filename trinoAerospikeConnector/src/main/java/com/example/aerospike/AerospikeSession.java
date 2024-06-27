package com.example.aerospike;
import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.policy.ClientPolicy;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AerospikeSession implements Closeable {

    private AerospikeClient client;
    private Map<String, Object> sessionData; // Example storage for session data

    public AerospikeSession(String host, int port) {
        ClientPolicy policy = new ClientPolicy();
        this.client = new AerospikeClient(policy, host, port);
        this.sessionData = new HashMap<>();
    }

    public String createSession() {
        String sessionId = generateSessionId();
        // Store session in Aerospike (example)
        Key key = new Key("session", "sessions", sessionId);
        Bin bin = new Bin("sessionData", sessionData);
        client.put(null, key, bin);
        return sessionId;
    }

    public void setSessionData(String sessionId, String key, Object value) {
        // Update session data in Aerospike (example)
        Key sessionKey = new Key("session", "sessions", sessionId);
        sessionData.put(key, value);
        Bin bin = new Bin("sessionData", sessionData);
        client.put(null, sessionKey, bin);
    }

    public Object getSessionData(String sessionId, String key) {
        // Retrieve session data from Aerospike (example)
        Key sessionKey = new Key("session", "sessions", sessionId);
        Record record = client.get(null, sessionKey);
        if (record != null) {
            Map<String, Object> data = (Map<String, Object>) record.getValue("sessionData");
            return data.get(key);
        }
        return null;
    }

    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    @Override
    public void close() throws IOException {
        if (client != null) {
            client.close();
        }
    }
}