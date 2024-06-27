package com.example.aerospike;
import io.prestosql.spi.connector.*;
import io.prestosql.spi.transaction.IsolationLevel;
import io.prestosql.spi.type.TypeManager;

import static io.prestosql.spi.transaction.IsolationLevel.READ_UNCOMMITTED;
import static io.prestosql.spi.transaction.IsolationLevel.checkConnectorSupports;

public class AerospikeConnector implements Connector{
    private final AerospikeConnection connection;
    private  AerospikeRecordSetProvider recordSetProvider;
    private  AerospikeMetadata metadata;
    private  AerospikeSplitManager splitManager;

    public AerospikeConnector(AerospikeConnection connection, TypeManager typeManager) {
        this.connection = connection;
    }



    @Override
    public ConnectorTransactionHandle beginTransaction(IsolationLevel isolationLevel, boolean readOnly) {

        checkConnectorSupports(READ_UNCOMMITTED, isolationLevel);
        return null;//AerospikeTransactionHandle.INSTANCE;
    }

    @Override
    public ConnectorMetadata getMetadata(ConnectorTransactionHandle transactionHandle) {
        return (ConnectorMetadata) metadata;
    }


    @Override
    public ConnectorSplitManager getSplitManager()
    {

        return (ConnectorSplitManager) splitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {

        return (ConnectorRecordSetProvider) recordSetProvider;
    }



    // Implement other Connector methods as needed
}
