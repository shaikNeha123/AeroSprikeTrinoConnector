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

import io.trino.plugin.Aerospike.AerospikeType.Kind;
import io.trino.spi.type.*;

import static io.trino.plugin.Aerospike.AerospikeType.primitiveType;
import static io.trino.spi.type.VarcharType.createUnboundedVarcharType;

public final class AerospikeTypes
{
    private AerospikeTypes() {}

    public static final AerospikeType ASCII = primitiveType(Kind.ASCII, createUnboundedVarcharType());
    public static final AerospikeType BIGINT = primitiveType(Kind.BIGINT, BigintType.BIGINT);
    public static final AerospikeType BLOB = primitiveType(Kind.BLOB, VarbinaryType.VARBINARY);
    public static final AerospikeType BOOLEAN = primitiveType(Kind.BOOLEAN, BooleanType.BOOLEAN);
    public static final AerospikeType COUNTER = primitiveType(Kind.COUNTER, BigintType.BIGINT);
    public static final AerospikeType CUSTOM = primitiveType(Kind.CUSTOM, VarbinaryType.VARBINARY);
    public static final AerospikeType DATE = primitiveType(Kind.DATE, DateType.DATE);
    public static final AerospikeType DECIMAL = primitiveType(Kind.DECIMAL, DoubleType.DOUBLE);
    public static final AerospikeType DOUBLE = primitiveType(Kind.DOUBLE, DoubleType.DOUBLE);
    public static final AerospikeType FLOAT = primitiveType(Kind.FLOAT, RealType.REAL);
    public static final AerospikeType INT = primitiveType(Kind.INT, IntegerType.INTEGER);
    public static final AerospikeType LIST = primitiveType(Kind.LIST, createUnboundedVarcharType());
    public static final AerospikeType MAP = primitiveType(Kind.MAP, createUnboundedVarcharType());
    public static final AerospikeType SET = primitiveType(Kind.SET, createUnboundedVarcharType());
    public static final AerospikeType SMALLINT = primitiveType(Kind.SMALLINT, SmallintType.SMALLINT);
    public static final AerospikeType TEXT = primitiveType(Kind.TEXT, createUnboundedVarcharType());
    public static final AerospikeType TIME = primitiveType(Kind.TIME, TimeType.TIME_NANOS);
    public static final AerospikeType TIMESTAMP = primitiveType(Kind.TIMESTAMP, TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS);
    public static final AerospikeType TIMEUUID = primitiveType(Kind.TIMEUUID, UuidType.UUID);
    public static final AerospikeType TINYINT = primitiveType(Kind.TINYINT, TinyintType.TINYINT);
    public static final AerospikeType UUID = primitiveType(Kind.UUID, UuidType.UUID);
    public static final AerospikeType VARCHAR = primitiveType(Kind.VARCHAR, createUnboundedVarcharType());
    public static final AerospikeType VARINT = primitiveType(Kind.VARINT, createUnboundedVarcharType());
}
