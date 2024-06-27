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

import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ConnectorSplit;
import io.trino.spi.connector.RecordCursor;
import io.trino.spi.connector.RecordSet;
import io.trino.spi.type.Type;

import java.util.List;
import java.util.function.Function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public class AerospikeRecordSet
        implements RecordSet
{
//    private final AerospikeSession cassandraSession;
//    private final AerospikeTypeManager cassandraTypeManager;
//    private final String cql;
//    private final List<String> cassandraNames;
//    private final List<AerospikeType> cassandraTypes;
//    private final List<Type> columnTypes;

//    public AerospikeRecordSet(){}
//    AerospikeSession aerospikeSession, AerospikeTypeManager aerospikeTypeManager, String cql, List<AerospikeColumnHandle> cassandraColumns)
//    {
//        this.cassandraSession = requireNonNull(aerospikeSession, "aerospikeSession is null");
//        this.cassandraTypeManager = requireNonNull(aerospikeTypeManager, "aerospikeTypeManager is null");
//        this.cql = requireNonNull(cql, "cql is null");
//
//        requireNonNull(cassandraColumns, "cassandraColumns is null");
//        this.cassandraNames = transformList(cassandraColumns, CassandraColumnHandle::name);
//        this.cassandraTypes = transformList(cassandraColumns, CassandraColumnHandle::cassandraType);
//        this.columnTypes = transformList(cassandraColumns, CassandraColumnHandle::getType);
//    }

    public AerospikeRecordSet(ConnectorSplit split, List<? extends ColumnHandle> columns) {
    }

    @Override
    public List<Type> getColumnTypes()
    {
        return null;//columnTypes;
    }

    @Override
    public RecordCursor cursor()
    {
        return null;//new AerospikeRecordCursor(aerospikeSession, aerospikeTypeManager, aerospikeNames, cassandraTypes, cql);
    }

    private static <T, R> List<R> transformList(List<T> list, Function<T, R> function)
    {
        return list.stream().map(function).collect(toImmutableList());
    }
}
