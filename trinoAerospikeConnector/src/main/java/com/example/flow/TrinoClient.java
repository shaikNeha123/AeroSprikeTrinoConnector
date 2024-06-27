package com.example.flow;

public class TrinoClient {
    public static void main(String[] args) {
        // Initialize Trino client and submit SQL query
        String query = "SELECT * FROM aerospike_table WHERE ...";
        TrinoCoordinator coordinator = new TrinoCoordinator();
        coordinator.executeQuery(query);
    }
}

