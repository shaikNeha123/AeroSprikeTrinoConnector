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
//    private final AerospikeSession aerospikeSession;
//    private final AerospikeTypeManager aerospikeTypeManager;
//    private final String cql;
//    private final List<String> aerospikeNames;
//    private final List<AerospikeType> aerospikeTypes;
//    private final List<Type> columnTypes;

//    public AerospikeRecordSet(){}
//    AerospikeSession aerospikeSession, AerospikeTypeManager aerospikeTypeManager, String cql, List<AerospikeColumnHandle> aerospikeColumns)
//    {
//        this.aerospikeSession = requireNonNull(aerospikeSession, "aerospikeSession is null");
//        this.aerospikeTypeManager = requireNonNull(aerospikeTypeManager, "aerospikeTypeManager is null");
//        this.cql = requireNonNull(cql, "cql is null");
//
//        requireNonNull(aerospikeColumns, "aerospikeColumns is null");
//        this.aerospikeNames = transformList(aerospikeColumns, AerospikeColumnHandle::name);
//        this.aerospikeTypes = transformList(aerospikeColumns, AerospikeColumnHandle::aerospikeType);
//        this.columnTypes = transformList(aerospikeColumns, AersopikeColumnHandle::getType);
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
        return null;//new AerospikeRecordCursor(aerospikeSession, aerospikeTypeManager, aerospikeNames, aerospikeTypes, cql);
    }

    private static <T, R> List<R> transformList(List<T> list, Function<T, R> function)
    {
        return list.stream().map(function).collect(toImmutableList());
    }
}
