/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.aerospike;

import com.google.inject.Inject;
import io.airlift.log.Logger;
import io.trino.spi.connector.*;

import java.util.List;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class AerospikeRecordSetProvider
        implements ConnectorRecordSetProvider
{
    private static final Logger log = Logger.get(AerospikeRecordSetProvider.class);

    private  AerospikeSession aerospikeSession;
//    private final AerospikeTypeManager aerospikeTypeManager;

    @Inject
    public AerospikeRecordSetProvider(AerospikeSession aerospikeSession)
    {
        this.aerospikeSession = requireNonNull(aerospikeSession, "aerospikeSession is null");
//        this.aerospikeTypeManager = requireNonNull(aerospikeTypeManager);
    }

    @Override
    public RecordSet getRecordSet(ConnectorTransactionHandle transaction, ConnectorSession session, ConnectorSplit split, ConnectorTableHandle table, List<? extends ColumnHandle> columns)
    {
        // Implement logic to create and return a RecordSet for the given split and columns
        // Example:
        return new AerospikeRecordSet(split, columns);
    }
}
