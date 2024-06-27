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
import com.google.inject.Inject;
import io.trino.spi.connector.*;

import java.util.*;
import java.util.function.UnaryOperator;
import static io.trino.spi.connector.RelationCommentMetadata.forRelation;
import static java.util.Locale.ENGLISH;

public class AerospikeMetadata
        implements ConnectorMetadata
{
    public static final String PRESTO_COMMENT_METADATA = "Presto Metadata:";

    private  AerospikeSession aerospikeSession;
//  private final boolean allowDropTable;

//    private final JsonCodec<List<ExtraColumnMetadata>> extraColumnMetadataCodec;
//    private final AerospikeTypeManager aerospikeTypeManager;

    @Inject
    public AerospikeMetadata(){}
//            Aerospikesession aerospikeSession,
//            CassandraPartitionManager partitionManager,
//            JsonCodec<List<ExtraColumnMetadata>> extraColumnMetadataCodec,
//            AerospikeTypeManager aerospikeTypeManager,
//            AerospikeClientConfig config)
//    {
//        this.partitionManager = requireNonNull(partitionManager, "partitionManager is null");
//        this.cassandraSession = requireNonNull(aerospikeSession, "aerospikeSession is null");
//        this.cassandraTypeManager = requireNonNull(aerospikeTypeManager, "aerospikeTypeManager is null");
//        this.allowDropTable = config.getAllowDropTable();
//        this.extraColumnMetadataCodec = requireNonNull(extraColumnMetadataCodec, "extraColumnMetadataCodec is null");
//    }

    @Override
    public List<String> listSchemaNames(ConnectorSession session)
    {
        return null;//
//        // aerospikeSession.getCaseSensitiveSchemaNames().stream()
//                .map(name -> name.toLowerCase(ENGLISH))
//                .collect(toImmutableList());
    }


    private static SchemaTableName getTableName(ConnectorTableHandle tableHandle)
    {
        return null;//((AerospikeTableHandle) tableHandle).getRequiredNamedRelation().getSchemaTableName();
    }

    @Override
    public ConnectorTableMetadata getTableMetadata(ConnectorSession session, ConnectorTableHandle tableHandle)
    {
//        requireNonNull(tableHandle, "tableHandle is null");
//        CassandraTableHandle handle = (CassandraTableHandle) tableHandle;
//        if (handle.relationHandle() instanceof AerospikeQueryRelationHandle queryRelationHandle) {
//            List<ColumnMetadata> columns = getColumnHandles(queryRelationHandle.getQuery()).stream()
//                    .map(AerospikeTableHandle.class::cast)
//                    .map(AerospikeColumnHandle::getColumnMetadata)
//                    .collect(toImmutableList());
//            return new ConnectorTableMetadata(getSchemaTableName(handle), columns);
//        }
        return getTableMetadata(getTableName(tableHandle));
    }

    private ConnectorTableMetadata getTableMetadata(SchemaTableName tableName)
    {
//        CassandraTable table = cassandraSession.getTable(tableName);
        List<ColumnMetadata> columns = null;
//        table.columns().stream()
//                .map(CassandraColumnHandle::getColumnMetadata)
//                .collect(toList());
        return new ConnectorTableMetadata(tableName, columns);
    }

    @Override
    public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> schemaName1)
    {
        ImmutableList.Builder<SchemaTableName> tableNames = ImmutableList.builder();
        List<String> schemaNames = listSchemas(session, schemaName1);
        for (String schemaName : schemaNames) {
            try {
//                for (String tableName : aerospikeSession.getCaseSensitiveTableNames(schemaName)) {
//                    tableNames.add(new SchemaTableName(schemaName, tableName.toLowerCase(ENGLISH)));
//                }
            }
            catch (SchemaNotFoundException e) {
                // schema disappeared during listing operation
            }
        }
        return tableNames.build();
    }

    private List<String> listSchemas(ConnectorSession session, Optional<String> schemaName)
    {
        return schemaName.map(ImmutableList::of)
                .orElseGet(() -> (ImmutableList<String>) listSchemaNames(session));
    }


    @Override
    public Iterator<RelationCommentMetadata> streamRelationComments(ConnectorSession session, Optional<String> schemaName, UnaryOperator<Set<SchemaTableName>> relationFilter)
    {
        Map<SchemaTableName, RelationCommentMetadata> relationColumns = new HashMap<>();

        for (SchemaTableName schemaTableName : listTables(session, schemaName)) {
            // Set empty comments because the connector doesn't support table comments
            relationColumns.put(schemaTableName, forRelation(schemaTableName, Optional.empty()));
        }

        return relationFilter.apply(relationColumns.keySet()).stream()
                .map(relationColumns::get)
                .iterator();
    }

    @Override
    public ColumnMetadata getColumnMetadata(ConnectorSession session, ConnectorTableHandle tableHandle, ColumnHandle columnHandle)
    {
        return null;//((AerospikeColumnHandle) columnHandle).getColumnMetadata();
    }



}
