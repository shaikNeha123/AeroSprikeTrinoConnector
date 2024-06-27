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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.nimbusds.oauth2.sdk.rar.DataType;
import io.airlift.slice.Slice;
import io.prestosql.client.Row;
import io.trino.spi.TrinoException;
import io.trino.spi.block.BlockBuilder;
import io.trino.spi.block.SqlRow;
import io.trino.spi.predicate.NullableValue;
import io.trino.spi.type.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Supplier;

import static com.example.aerospike.AerospikeTypes.*;
import static com.fasterxml.jackson.databind.util.ClassUtil.primitiveType;
import static com.google.common.base.Verify.verify;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.net.InetAddresses.toAddrString;
import static io.airlift.slice.Slices.*;
import static io.trino.plugin.aerospike.AerospikeType.Kind.*;
import static io.trino.plugin.aerospike.AerospikeType.primitiveType;
import static io.trino.plugin.aerospike.util.AerospikeCqlUtils.quoteStringLiteral;
import static io.trino.plugin.aerospike.util.AerospikeCqlUtils.quoteStringLiteralForJson;
import static io.trino.spi.StandardErrorCode.*;
import static io.trino.spi.block.RowValueBuilder.buildRowValue;
import static io.trino.spi.type.DateTimeEncoding.unpackMillisUtc;
import static io.trino.spi.type.Timestamps.PICOSECONDS_PER_NANOSECOND;
import static io.trino.spi.type.Timestamps.roundDiv;
import static io.trino.spi.type.TypeUtils.writeNativeValue;
import static io.trino.spi.type.UuidType.javaUuidToTrinoUuid;
import static io.trino.spi.type.UuidType.trinoUuidToJavaUuid;
import static java.lang.Float.floatToRawIntBits;
import static java.lang.Float.intBitsToFloat;
import static java.lang.System.arraycopy;
import static java.util.Objects.requireNonNull;

public class AerospikeTypeManager
{
    private final Type ipAddressType;

    @Inject
    public AerospikeTypeManager(TypeManager typeManager)
    {
        requireNonNull(typeManager, "typeManager is null");
        this.ipAddressType = typeManager.getType(new TypeSignature(StandardTypes.IPADDRESS));
    }

    public Optional<AerospikeType> toAerospikeType(DataType dataType)
    {
        return switch (dataType.getProtocolCode()) {
            case ProtocolConstants.DataType.ASCII -> Optional.of(AerospikeTypes.ASCII);
            case ProtocolConstants.DataType.BIGINT -> Optional.of(AerospikeTypes.INTEGER);
            case ProtocolConstants.DataType.BLOB -> Optional.of(AerospikeTypes.BLOB);
            case ProtocolConstants.DataType.BOOLEAN -> Optional.of(AerospikeTypes.BOOLEAN);
            case ProtocolConstants.DataType.DATE -> Optional.of(DATE);
            case ProtocolConstants.DataType.DOUBLE -> Optional.of(AerospikeTypes.DOUBLE);
            case ProtocolConstants.DataType.FLOAT -> Optional.of(AerospikeTypes.FLOAT);
            case ProtocolConstants.DataType.INET -> Optional.of(primitiveType(AerospikeType.Kind.INET, ipAddressType));
            case ProtocolConstants.DataType.LIST -> Optional.of(AerospikeTypes.LIST);
            case ProtocolConstants.DataType.MAP -> Optional.of(AerospikeTypes.MAP);
            case ProtocolConstants.DataType.SET -> Optional.of(AerospikeTypes.SET);
            case ProtocolConstants.DataType.SMALLINT -> Optional.of(AerospikeTypes.SMALLINT);
            case ProtocolConstants.DataType.TIME -> Optional.of(TIME);
            case ProtocolConstants.DataType.TIMESTAMP -> Optional.of(TIMESTAMP);
            case ProtocolConstants.DataType.TUPLE -> createTypeForTuple(dataType);
            case ProtocolConstants.DataType.UDT -> createTypeForUserType(dataType);
            case ProtocolConstants.DataType.UUID -> Optional.of(AerospikeTypes.UUID);
            default -> Optional.empty();
        };
    }

    private Optional<AerospikeType> createTypeForTuple(DataType dataType)
    {
        TupleType tupleType = (TupleType) dataType;
        List<Optional<AerospikeType>> argumentTypesOptionals = tupleType.getComponentTypes().stream()
                .map(this::toAerospikeType)
                .collect(toImmutableList());

        if (argumentTypesOptionals.stream().anyMatch(Optional::isEmpty)) {
            return Optional.empty();
        }

        List<AerospikeType> argumentTypes = argumentTypesOptionals.stream()
                .map(Optional::get)
                .collect(toImmutableList());

        RowType trinoType = RowType.anonymous(
                argumentTypes.stream()
                        .map(AerospikeType::trinoType)
                        .collect(toImmutableList()));

        return Optional.of(new AerospikeType(TUPLE, trinoType, argumentTypes));
    }

    private Optional<AerospikeType> createTypeForUserType(DataType dataType)
    {
        UserDefinedType userDefinedType = (UserDefinedType) dataType;
        // Using ImmutableMap is important as we exploit the fact that entries iteration order matches the order of putting values via builder
        ImmutableMap.Builder<String, AerospikeType> argumentTypes = ImmutableMap.builder();

        List<CqlIdentifier> fieldNames = userDefinedType.getFieldNames();
        List<DataType> fieldTypes = userDefinedType.getFieldTypes();
        if (fieldNames.size() != fieldTypes.size()) {
            throw new TrinoException(GENERIC_INTERNAL_ERROR, format("Mismatch between the number of field names (%s) and the number of field types (%s) for the data type %s", fieldNames.size(), fieldTypes.size(), dataType));
        }
        for (int i = 0; i < fieldNames.size(); i++) {
            Optional<AerospikeType> AerospikeType = toAerospikeType(fieldTypes.get(i));
            if (AerospikeType.isEmpty()) {
                return Optional.empty();
            }
            argumentTypes.put(fieldNames.get(i).toString(), AerospikeType.get());
        }

        RowType trinoType = RowType.from(
                argumentTypes.buildOrThrow().entrySet().stream()
                        .map(field -> new RowType.Field(Optional.of(field.getKey()), field.getValue().trinoType()))
                        .collect(toImmutableList()));

        return Optional.of(new AerospikeType(UDT, trinoType, ImmutableList.copyOf(argumentTypes.buildOrThrow().values())));
    }

    public NullableValue getColumnValue(AerospikeType AerospikeType, Row row, int position)
    {
        return getColumnValue(AerospikeType, row, position, () -> row.getColumnDefinitions().get(position).getType());
    }

    public NullableValue getColumnValue(AerospikeType AerospikeType, GettableByIndex row, int position, Supplier<DataType> dataTypeSupplier)
    {
        Type trinoType = AerospikeType.trinoType();
        if (row.isNull(position)) {
            return NullableValue.asNull(trinoType);
        }

        return switch (AerospikeType.kind()) {
            case ASCII, TEXT, VARCHAR -> NullableValue.of(trinoType, utf8Slice(row.getString(position)));
            case INT -> NullableValue.of(trinoType, (long) row.getInt(position));
            case SMALLINT -> NullableValue.of(trinoType, (long) row.getShort(position));
            case TINYINT -> NullableValue.of(trinoType, (long) row.getByte(position));
            case BIGINT, COUNTER -> NullableValue.of(trinoType, row.getLong(position));
            case BOOLEAN -> NullableValue.of(trinoType, row.getBoolean(position));
            case DOUBLE -> NullableValue.of(trinoType, row.getDouble(position));
            case FLOAT -> NullableValue.of(trinoType, (long) floatToRawIntBits(row.getFloat(position)));
            case DECIMAL -> NullableValue.of(trinoType, row.getBigDecimal(position).doubleValue());
            case UUID, TIMEUUID -> NullableValue.of(trinoType, javaUuidToTrinoUuid(row.getUuid(position)));
            case TIME -> NullableValue.of(trinoType, row.getLocalTime(position).toNanoOfDay() * PICOSECONDS_PER_NANOSECOND);
            case TIMESTAMP -> NullableValue.of(trinoType, packDateTimeWithZone(row.getInstant(position).toEpochMilli(), TimeZoneKey.UTC_KEY));
            case DATE -> NullableValue.of(trinoType, row.getLocalDate(position).toEpochDay());
            case INET -> NullableValue.of(trinoType, castFromVarcharToIpAddress(utf8Slice(toAddrString(row.getInetAddress(position)))));
            case VARINT -> NullableValue.of(trinoType, utf8Slice(row.getBigInteger(position).toString()));
            case BLOB, CUSTOM -> NullableValue.of(trinoType, wrappedHeapBuffer(row.getBytesUnsafe(position)));
            case SET -> NullableValue.of(trinoType, utf8Slice(buildArrayValueFromSetType(row, position, dataTypeSupplier.get())));
            case LIST -> NullableValue.of(trinoType, utf8Slice(buildArrayValueFromListType(row, position, dataTypeSupplier.get())));
            case MAP -> NullableValue.of(trinoType, utf8Slice(buildMapValue(row, position, dataTypeSupplier.get())));
            case TUPLE -> NullableValue.of(trinoType, buildTupleValue(AerospikeType, row, position));
            case UDT -> NullableValue.of(trinoType, buildUserTypeValue(AerospikeType, row, position));
        };
    }

    private String buildMapValue(GettableByIndex row, int position, DataType dataType)
    {
        checkArgument(dataType instanceof MapType, "Expected to deal with an instance of %s class, got: %s", MapType.class, dataType);
        MapType mapType = (MapType) dataType;
        return buildMapValue((Map<?, ?>) row.getObject(position), mapType.getKeyType(), mapType.getValueType());
    }

    private String buildMapValue(Map<?, ?> AerospikeMap, DataType keyType, DataType valueType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (Map.Entry<?, ?> entry : AerospikeMap.entrySet()) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(objectToJson(entry.getKey(), keyType));
            sb.append(":");
            sb.append(objectToJson(entry.getValue(), valueType));
        }
        sb.append("}");
        return sb.toString();
    }

    private String buildArrayValueFromSetType(GettableByIndex row, int position, DataType type)
    {
        checkArgument(type instanceof SetType, "Expected to deal with an instance of %s class, got: %s", SetType.class, type);
        SetType setType = (SetType) type;
        return buildArrayValue((Collection<?>) row.getObject(position), setType.getElementType());
    }

    private String buildArrayValueFromListType(GettableByIndex row, int position, DataType type)
    {
        checkArgument(type instanceof ListType, "Expected to deal with an instance of %s class, got: %s", ListType.class, type);
        ListType listType = (ListType) type;
        return buildArrayValue((Collection<?>) row.getObject(position), listType.getElementType());
    }

    @VisibleForTesting
    String buildArrayValue(Collection<?> AerospikeCollection, DataType elementType)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object value : AerospikeCollection) {
            if (sb.length() > 1) {
                sb.append(",");
            }
            sb.append(objectToJson(value, elementType));
        }
        sb.append("]");
        return sb.toString();
    }

    private SqlRow buildTupleValue(AerospikeType type, GettableByIndex row, int position)
    {
        verify(type.kind() == TUPLE, "Not a TUPLE type");
        TupleValue tupleValue = row.getTupleValue(position);
        return buildRowValue((RowType) type.trinoType(), fieldBuilders -> {
            int tuplePosition = 0;
            List<AerospikeType> argumentTypes = type.argumentTypes();
            for (int i = 0; i < argumentTypes.size(); i++) {
                AerospikeType argumentType = argumentTypes.get(i);
                BlockBuilder fieldBuilder = fieldBuilders.get(i);
                int finalTuplePosition = tuplePosition;
                NullableValue value = getColumnValue(argumentType, tupleValue, tuplePosition, () -> tupleValue.getType().getComponentTypes().get(finalTuplePosition));
                writeNativeValue(argumentType.trinoType(), fieldBuilder, value.getValue());
                tuplePosition++;
            }
        });
    }

    private SqlRow buildUserTypeValue(AerospikeType type, GettableByIndex row, int position)
    {
        verify(type.kind() == UDT, "Not a user defined type: %s", type.kind());
        UdtValue udtValue = row.getUdtValue(position);
        return buildRowValue((RowType) type.trinoType(), fieldBuilders -> {
            int tuplePosition = 0;
            List<DataType> udtTypeFieldTypes = udtValue.getType().getFieldTypes();
            List<AerospikeType> argumentTypes = type.argumentTypes();
            for (int i = 0; i < argumentTypes.size(); i++) {
                AerospikeType argumentType = argumentTypes.get(i);
                BlockBuilder fieldBuilder = fieldBuilders.get(i);
                int finalTuplePosition = tuplePosition;
                NullableValue value = getColumnValue(argumentType, udtValue, tuplePosition, () -> udtTypeFieldTypes.get(finalTuplePosition));
                writeNativeValue(argumentType.trinoType(), fieldBuilder, value.getValue());
                tuplePosition++;
            }
        });
    }

    // TODO unify with toCqlLiteral
    public String getColumnValueForCql(AerospikeType type, Row row, int position)
    {
        if (row.isNull(position)) {
            return null;
        }

        return switch (type.kind()) {
            case ASCII, TEXT, VARCHAR -> quoteStringLiteral(row.getString(position));
            case INT -> Integer.toString(row.getInt(position));
            case SMALLINT -> Short.toString(row.getShort(position));
            case TINYINT -> Byte.toString(row.getByte(position));
            case BIGINT, COUNTER -> Long.toString(row.getLong(position));
            case BOOLEAN -> Boolean.toString(row.getBoolean(position));
            case DOUBLE -> Double.toString(row.getDouble(position));
            case FLOAT -> Float.toString(row.getFloat(position));
            case DECIMAL -> row.getBigDecimal(position).toString();
            case UUID, TIMEUUID -> row.getUuid(position).toString();
            case TIME -> quoteStringLiteral(row.getLocalTime(position).toString());
            case TIMESTAMP -> Long.toString(row.getInstant(position).toEpochMilli());
            case DATE -> quoteStringLiteral(row.getLocalDate(position).toString());
            case INET -> quoteStringLiteral(toAddrString(row.getInetAddress(position)));
            case VARINT -> row.getBigInteger(position).toString();
            case BLOB, CUSTOM -> Bytes.toHexString(row.getBytesUnsafe(position));
            case LIST, SET, MAP, TUPLE, UDT -> throw new IllegalStateException("Handling of type " + this + " is not implemented");
        };
    }

    // TODO unify with getColumnValueForCql
    public String toCqlLiteral(AerospikeType type, Object trinoNativeValue)
    {
        AerospikeType.Kind kind = type.kind();
        if (kind == DATE) {
            LocalDate date = LocalDate.ofEpochDay((long) trinoNativeValue);
            return quoteStringLiteral(date.toString());
        }
        if (kind == TIME) {
            LocalTime time = LocalTime.ofNanoOfDay(roundDiv((long) trinoNativeValue, PICOSECONDS_PER_NANOSECOND));
            return quoteStringLiteral(time.toString());
        }
        if (kind == TIMESTAMP) {
            return String.valueOf(unpackMillisUtc((Long) trinoNativeValue));
        }

        String value;
        if (trinoNativeValue instanceof Slice) {
            value = ((Slice) trinoNativeValue).toStringUtf8();
        }
        else {
            value = trinoNativeValue.toString();
        }

        return switch (kind) {
            case ASCII, TEXT, VARCHAR -> quoteStringLiteral(value);
            case INET -> quoteStringLiteral(value.substring(1)); // remove '/' in the string. e.g. /127.0.0.1
            default -> value;
        };
    }

    private String objectToJson(Object AerospikeValue, DataType dataType)
    {
        AerospikeType AerospikeType = toAerospikeType(dataType)
                .orElseThrow(() -> new IllegalStateException("Unsupported type: " + dataType));

        switch (AerospikeType.kind()) {
            case ASCII:
            case TEXT:
            case VARCHAR:
            case UUID:
            case TIMEUUID:
            case TIME:
            case TIMESTAMP:
            case DATE:
            case INET:
            case VARINT:
            case TUPLE:
                return quoteStringLiteralForJson(AerospikeValue.toString());
            case UDT:
                return quoteStringLiteralForJson(((UdtValue) AerospikeValue).getFormattedContents());

            case BLOB:
            case CUSTOM:
                return quoteStringLiteralForJson(Bytes.toHexString((ByteBuffer) AerospikeValue));

            case SMALLINT:
            case TINYINT:
            case INT:
            case BIGINT:
            case COUNTER:
            case BOOLEAN:
            case DOUBLE:
            case FLOAT:
            case DECIMAL:
                return AerospikeValue.toString();
            case LIST:
                checkArgument(dataType instanceof ListType, "Expected to deal with an instance of %s class, got: %s", ListType.class, dataType);
                ListType listType = (ListType) dataType;
                return buildArrayValue((Collection<?>) AerospikeValue, listType.getElementType());
            case SET:
                checkArgument(dataType instanceof SetType, "Expected to deal with an instance of %s class, got: %s", SetType.class, dataType);
                SetType setType = (SetType) dataType;
                return buildArrayValue((Collection<?>) AerospikeValue, setType.getElementType());
            case MAP:
                checkArgument(dataType instanceof MapType, "Expected to deal with an instance of %s class, got: %s", MapType.class, dataType);
                MapType mapType = (MapType) dataType;
                return buildMapValue((Map<?, ?>) AerospikeValue, mapType.getKeyType(), mapType.getValueType());
        }
        throw new IllegalStateException("Unsupported type: " + AerospikeType);
    }

    public Object getJavaValue(AerospikeType.Kind kind, Object trinoNativeValue)
    {
        switch (kind) {
            case ASCII:
            case TEXT:
            case VARCHAR:
                return ((Slice) trinoNativeValue).toStringUtf8();
            case BIGINT:
            case BOOLEAN:
            case DOUBLE:
            case COUNTER:
                return trinoNativeValue;
            case INET:
                try {
                    return InetAddress.getByAddress(((Slice) trinoNativeValue).getBytes());
                }
                catch (UnknownHostException e) {
                    throw new TrinoException(INVALID_CAST_ARGUMENT, "Invalid IP address binary length: " + ((Slice) trinoNativeValue).length(), e);
                }
            case INT:
            case SMALLINT:
            case TINYINT:
                return ((Long) trinoNativeValue).intValue();
            case FLOAT:
                // conversion can result in precision lost
                return intBitsToFloat(((Long) trinoNativeValue).intValue());
            case DECIMAL:
                // conversion can result in precision lost
                // Trino uses double for decimal, so to keep the floating point precision, convert it to string.
                // Otherwise partition id doesn't match
                return new BigDecimal(trinoNativeValue.toString());
            case TIME:
                return LocalTime.ofNanoOfDay(roundDiv((long) trinoNativeValue, PICOSECONDS_PER_NANOSECOND));
            case TIMESTAMP:
                return Instant.ofEpochMilli(unpackMillisUtc((Long) trinoNativeValue));
            case DATE:
                return LocalDate.ofEpochDay((Long) trinoNativeValue);
            case UUID:
            case TIMEUUID:
                return trinoUuidToJavaUuid((Slice) trinoNativeValue);
            case BLOB:
            case CUSTOM:
            case TUPLE:
            case UDT:
                return ((Slice) trinoNativeValue).toStringUtf8();
            case VARINT:
                return new BigInteger(((Slice) trinoNativeValue).toStringUtf8());
            case SET:
            case LIST:
            case MAP:
        }
        throw new IllegalStateException("Back conversion not implemented for " + this);
    }

    public boolean isFullySupported(DataType dataType)
    {
        if (toAerospikeType(dataType).isEmpty()) {
            return false;
        }

        if (dataType instanceof UserDefinedType userDefinedType) {
            return userDefinedType.getFieldTypes().stream()
                    .allMatch(this::isFullySupported);
        }

        if (dataType instanceof MapType mapType) {
            return Arrays.stream(new DataType[] {mapType.getKeyType(), mapType.getValueType()})
                    .allMatch(this::isFullySupported);
        }

        if (dataType instanceof ListType listType) {
            return isFullySupported(listType.getElementType());
        }

        if (dataType instanceof TupleType tupleType) {
            return tupleType.getComponentTypes().stream()
                    .allMatch(this::isFullySupported);
        }

        if (dataType instanceof SetType setType) {
            return isFullySupported(setType.getElementType());
        }

        return true;
    }

    public AerospikeType toAerospikeType(Type type, ProtocolVersion protocolVersion)
    {
        if (type.equals(BooleanType.BOOLEAN)) {
            return AerospikeTypes.BOOLEAN;
        }
        if (type.equals(BigintType.BIGINT)) {
            return AerospikeTypes.BIGINT;
        }
        if (type.equals(IntegerType.INTEGER)) {
            return AerospikeTypes.INT;
        }
        if (type.equals(SmallintType.SMALLINT)) {
            return AerospikeTypes.SMALLINT;
        }
        if (type.equals(TinyintType.TINYINT)) {
            return AerospikeTypes.TINYINT;
        }
        if (type.equals(DoubleType.DOUBLE)) {
            return AerospikeTypes.DOUBLE;
        }
        if (type.equals(RealType.REAL)) {
            return AerospikeTypes.FLOAT;
        }
        if (type instanceof VarcharType) {
            return AerospikeTypes.TEXT;
        }
        if (type.equals(DateType.DATE)) {
            return protocolVersion.getCode() <= ProtocolVersion.V3.getCode()
                    ? AerospikeTypes.TEXT
                    : DATE;
        }
        if (type.equals(VarbinaryType.VARBINARY)) {
            return AerospikeType.BLOB;
        }
        if (type.equals(TimeType.TIME_NANOS)) {
            return AerospikeType.TIME;
        }
        if (type.equals(TimestampWithTimeZoneType.TIMESTAMP_TZ_MILLIS)) {
            return AerospikeType.TIMESTAMP;
        }
        if (type.equals(UuidType.UUID)) {
            return AerospikeType.UUID;
        }
        if (type.equals(ipAddressType)) {
            return primitiveType(
                    AerospikeType.Kind.INET,
                    ipAddressType);
        }
        throw new TrinoException(NOT_SUPPORTED, "Unsupported type: " + type);
    }

    public boolean isIpAddressType(Type type)
    {
        return type.equals(ipAddressType);
    }

    // This is a copy of IpAddressOperators.castFromVarcharToIpAddress method
    private static Slice castFromVarcharToIpAddress(Slice slice)
    {
        byte[] address;
        try {
            address = InetAddresses.forString(slice.toStringUtf8()).getAddress();
        }
        catch (IllegalArgumentException e) {
            throw new TrinoException(INVALID_CAST_ARGUMENT, "Cannot cast value to IPADDRESS: " + slice.toStringUtf8());
        }

        byte[] bytes;
        if (address.length == 4) {
            bytes = new byte[16];
            bytes[10] = (byte) 0xff;
            bytes[11] = (byte) 0xff;
            arraycopy(address, 0, bytes, 12, 4);
        }
        else if (address.length == 16) {
            bytes = address;
        }
        else {
            throw new TrinoException(GENERIC_INTERNAL_ERROR, "Invalid InetAddress length: " + address.length);
        }

        return wrappedBuffer(bytes);
    }
}
