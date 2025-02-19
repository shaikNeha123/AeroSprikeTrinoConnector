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


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.trino.plugin.aerospike.util.AerospikeCqlUtils;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public record AerospikeColumnHandle(
        String name,
        int ordinalPosition,
        AerospikeType AerospikeType,
        boolean partitionKey,
        boolean clusteringKey,
        boolean indexed,
        boolean hidden)
        implements ColumnHandle
{
    public AerospikeColumnHandle
    {
        requireNonNull(name, "name is null");
        checkArgument(ordinalPosition >= 0, "ordinalPosition is negative");
        requireNonNull(AerospikeType, "AerospikeType is null");
    }

    @JsonIgnore
    public ColumnMetadata getColumnMetadata()
    {
        return ColumnMetadata.builder()
                .setName(AerospikeCqlUtils.cqlNameToSqlName(name))
                .setType(AerospikeType.trinoType())
                .setHidden(hidden)
                .build();
    }

    @JsonIgnore
    public Type getType()
    {
        return AerospikeType.trinoType();
    }
}
