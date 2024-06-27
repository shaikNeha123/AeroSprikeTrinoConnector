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


import com.google.common.collect.ImmutableList;
import io.micrometer.observation.transport.Kind;
import io.trino.spi.type.Type;

import java.util.List;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public record AerospikeType(Kind kind, Type trinoType, List<AerospikeType> argumentTypes)
{
    public enum Kind
    {
        BOOLEAN(true),
        TINYINT(true),
        SMALLINT(true),
        INTEGER(true),
        BIGINT(true),
        FLOAT(true),
        DOUBLE(true),
        DECIMAL(true),
        DATE(true),
        TIME(true),
        TIMESTAMP(true),
        ASCII(true),
        TEXT(true),
        VARCHAR(true),
        BLOB(false),
        UUID(true),
        TIMEUUID(true),
        COUNTER(false),
        VARINT(false),
        INET(true),
        CUSTOM(false),
        LIST(false),
        SET(false),
        MAP(false),
        TUPLE(false),
        UDT(false),
        /**/;

        private final boolean supportedPartitionKey;

        Kind(boolean supportedPartitionKey)
        {
            this.supportedPartitionKey = supportedPartitionKey;
        }

        public boolean isSupportedPartitionKey()
        {
            return supportedPartitionKey;
        }
    }

    @Override
    public String toString()
    {
        String result = format("%s(%s", kind, trinoType);
        if (!argumentTypes.isEmpty()) {
            result += "; " + argumentTypes;
        }
        result += ")";
        return result;
    }
}
