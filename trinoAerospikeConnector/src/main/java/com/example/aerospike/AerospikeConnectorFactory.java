package com.example.aerospike;
import io.prestosql.spi.connector.*;

import java.util.Map;
public class AerospikeConnectorFactory  implements ConnectorFactory {

    @Override
    public String getName() {
        return "aerospike";
    }

    @Override
    public Connector create(String connectorId, Map<String, String> config, ConnectorContext context) {
        AerospikeConfig aerospikeConfig = new AerospikeConfig(config);
        AerospikeConnection aerospikeConnection = new AerospikeConnection(aerospikeConfig);
        return new AerospikeConnector(aerospikeConnection, context.getTypeManager());
    }
}
