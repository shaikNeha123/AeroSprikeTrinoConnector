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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.trino.plugin.cassandra.CassandraPartition;
import io.trino.spi.HostAddress;
import io.trino.spi.connector.ConnectorSplit;

import java.util.List;
import java.util.Map;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.airlift.slice.SizeOf.estimatedSizeOf;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

public class AerospikeSplit
        implements ConnectorSplit
{
    private static final int INSTANCE_SIZE = instanceSize(AerospikeSplit.class);

    private static int instanceSize(Class<AerospikeSplit> aerospikeSplitClass) {
        return 0;
    }

    private final String partitionId;
    private final List<HostAddress> addresses;
    private final String splitCondition;

    @JsonCreator
    public AerospikeSplit(
            @JsonProperty("partitionId") String partitionId,
            @JsonProperty("splitCondition") String splitCondition,
            @JsonProperty("addresses") List<HostAddress> addresses)
    {
        requireNonNull(partitionId, "partitionId is null");
        requireNonNull(addresses, "addresses is null");

        this.partitionId = partitionId;
        this.addresses = ImmutableList.copyOf(addresses);
        this.splitCondition = splitCondition;
    }

    @JsonProperty
    public String getSplitCondition()
    {
        return splitCondition;
    }

    @JsonProperty
    public String getPartitionId()
    {
        return partitionId;
    }

    @JsonProperty
    @Override
    public List<HostAddress> getAddresses()
    {
        return addresses;
    }

    @Override
    public Map<String, String> getSplitInfo()
    {
        return ImmutableMap.<String, String>builder()
                .put("hosts", addresses.stream().map(HostAddress::toString).collect(joining(",")))
                .put("partitionId", partitionId)
                .build();
    }

    @Override
    public long getRetainedSizeInBytes()
    {
        return INSTANCE_SIZE
                + estimatedSizeOf(partitionId)
                + estimatedSizeOf(addresses, HostAddress::getRetainedSizeInBytes)
                + estimatedSizeOf(splitCondition);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(partitionId)
                .toString();
    }

    public String getWhereClause()
    {
        if (partitionId.equals("<UNPARTITIONED>")) {
            if (splitCondition != null) {
                return " WHERE " + splitCondition;
            }
            return "";
        }
        if (splitCondition != null) {
            return " WHERE " + partitionId + " AND " + splitCondition;
        }
        return " WHERE " + partitionId;
    }
}
