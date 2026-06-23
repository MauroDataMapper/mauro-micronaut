package org.maurodata.service.semantic

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

import javax.sql.DataSource
import java.security.MessageDigest
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant

@CompileStatic
@Singleton
class SemanticRepository {

    private final DataSource dataSource

    SemanticRepository(DataSource dataSource) {
        this.dataSource = dataSource
    }

    EmbeddingProfile findProfileByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null
        }
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT id, name, provider, embedding_model, dimension, distance_metric, description
                 FROM semantic.embedding_profile
                 WHERE name = ? AND enabled = TRUE
             ''')) {
            statement.setString(1, name)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() ? profileFrom(rs) : null
            }
        }
    }

    List<EmbeddingProfile> profilesForIndex(String indexName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT p.id, p.name, p.provider, p.embedding_model, p.dimension, p.distance_metric, p.description
                 FROM semantic.embedding_profile p
                      JOIN semantic.semantic_index_profile ip ON ip.embedding_profile_id = p.id
                      JOIN semantic.semantic_index i ON i.id = ip.semantic_index_id
                 WHERE i.name = ? AND p.enabled = TRUE
                 ORDER BY p.name
             ''')) {
            statement.setString(1, indexName ?: 'catalogue-items-default')
            try (ResultSet rs = statement.executeQuery()) {
                List<EmbeddingProfile> profiles = new ArrayList<EmbeddingProfile>()
                while (rs.next()) {
                    profiles.add(profileFrom(rs))
                }
                profiles
            }
        }
    }

    List<Map<String, Object>> profiles() {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT p.id,
                        p.name,
                        p.provider,
                        p.embedding_model,
                        p.dimension,
                        p.distance_metric,
                        p.enabled,
                        p.description,
                        COALESCE(array_agg(i.name ORDER BY i.name) FILTER (WHERE i.name IS NOT NULL), ARRAY[]::text[]) AS indexes
                 FROM semantic.embedding_profile p
                      LEFT JOIN semantic.semantic_index_profile ip ON ip.embedding_profile_id = p.id
                      LEFT JOIN semantic.semantic_index i ON i.id = ip.semantic_index_id
                 GROUP BY p.id, p.name, p.provider, p.embedding_model, p.dimension, p.distance_metric, p.enabled, p.description
                 ORDER BY p.name
             ''')) {
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> profiles = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    profiles.add([
                        id: String.valueOf(rs.getObject('id')),
                        name: rs.getString('name'),
                        provider: rs.getString('provider'),
                        embeddingModel: rs.getString('embedding_model'),
                        dimension: rs.getInt('dimension'),
                        distanceMetric: rs.getString('distance_metric'),
                        enabled: rs.getBoolean('enabled'),
                        description: rs.getString('description'),
                        indexes: arrayToList(rs.getArray('indexes')?.array)
                    ] as Map<String, Object>)
                }
                profiles
            }
        }
    }

    List<Map<String, Object>> indexes() {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT i.id,
                        i.name,
                        i.status,
                        i.last_indexed_at,
                        c.name AS corpus_name,
                        c.source AS corpus_source,
                        COALESCE(array_agg(p.name ORDER BY p.name) FILTER (WHERE p.name IS NOT NULL), ARRAY[]::text[]) AS profiles,
                        COALESCE(array_agg(p.name ORDER BY p.name) FILTER (WHERE p.name IS NOT NULL AND p.enabled = TRUE), ARRAY[]::text[]) AS enabled_profiles,
                        (SELECT count(*) FROM semantic.semantic_chunk chunk WHERE chunk.corpus_id = i.corpus_id) AS chunks,
                        (SELECT count(*)
                         FROM semantic.semantic_embedding e
                              JOIN semantic.semantic_chunk chunk ON chunk.id = e.chunk_id
                         WHERE chunk.corpus_id = i.corpus_id) AS embeddings
                 FROM semantic.semantic_index i
                      JOIN semantic.semantic_corpus c ON c.id = i.corpus_id
                      LEFT JOIN semantic.semantic_index_profile ip ON ip.semantic_index_id = i.id
                      LEFT JOIN semantic.embedding_profile p ON p.id = ip.embedding_profile_id
                 GROUP BY i.id, i.name, i.status, i.last_indexed_at, c.name, c.source, i.corpus_id
                 ORDER BY i.name
             ''')) {
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> indexes = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    indexes.add([
                        id: String.valueOf(rs.getObject('id')),
                        name: rs.getString('name'),
                        status: rs.getString('status'),
                        lastIndexedAt: rs.getTimestamp('last_indexed_at')?.toInstant()?.toString(),
                        corpusName: rs.getString('corpus_name'),
                        corpusSource: rs.getString('corpus_source'),
                        profiles: arrayToList(rs.getArray('profiles')?.array),
                        enabledProfiles: arrayToList(rs.getArray('enabled_profiles')?.array),
                        chunks: rs.getLong('chunks'),
                        embeddings: rs.getLong('embeddings')
                    ] as Map<String, Object>)
                }
                indexes
            }
        }
    }

    Map<String, Object> createIndex(String indexName, String corpusName = 'catalogue-items') {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_index (name, corpus_id, status)
                 SELECT ?, c.id, 'STALE'
                 FROM semantic.semantic_corpus c
                 WHERE c.name = ?
                 ON CONFLICT (name) DO UPDATE
                 SET updated_at = now()
                 RETURNING id, name, status
             ''')) {
            statement.setString(1, indexName)
            statement.setString(2, corpusName ?: 'catalogue-items')
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic corpus named ${corpusName}")
                }
                [
                    id: String.valueOf(rs.getObject('id')),
                    name: rs.getString('name'),
                    status: rs.getString('status'),
                    corpusName: corpusName ?: 'catalogue-items'
                ] as Map<String, Object>
            }
        }
    }

    Map<String, Object> deleteIndex(String indexName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('DELETE FROM semantic.semantic_index WHERE name = ?')) {
            statement.setString(1, indexName)
            deleted = statement.executeUpdate()
        }
        [indexName: indexName, deleted: deleted] as Map<String, Object>
    }

    Map<String, Object> createProfile(Map<String, Object> request) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.embedding_profile (name, provider, embedding_model, dimension, distance_metric, enabled, description)
                 VALUES (?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT (name) DO UPDATE
                 SET provider = EXCLUDED.provider,
                     embedding_model = EXCLUDED.embedding_model,
                     dimension = EXCLUDED.dimension,
                     distance_metric = EXCLUDED.distance_metric,
                     enabled = EXCLUDED.enabled,
                     description = EXCLUDED.description,
                     updated_at = now()
                 RETURNING id, name, provider, embedding_model, dimension, distance_metric, enabled, description
             ''')) {
            statement.setString(1, requiredString(request, 'name'))
            statement.setString(2, requiredString(request, 'provider'))
            statement.setString(3, requiredString(request, 'embeddingModel'))
            statement.setInt(4, requiredInteger(request, 'dimension'))
            statement.setString(5, stringValue(request, 'distanceMetric') ?: 'cosine')
            statement.setBoolean(6, booleanValue(request, 'enabled', false))
            statement.setString(7, stringValue(request, 'description'))
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                profileMap(rs)
            }
        }
    }

    Map<String, Object> deleteProfile(String profileName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('DELETE FROM semantic.embedding_profile WHERE name = ?')) {
            statement.setString(1, profileName)
            deleted = statement.executeUpdate()
        }
        [profileName: profileName, deleted: deleted] as Map<String, Object>
    }

    Map<String, Object> setProfileEnabled(String profileName, boolean enabled) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.embedding_profile
                 SET enabled = ?,
                     updated_at = now()
                 WHERE name = ?
                 RETURNING id, name, provider, embedding_model, dimension, distance_metric, enabled, description
             ''')) {
            statement.setBoolean(1, enabled)
            statement.setString(2, profileName)
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic embedding profile named ${profileName}")
                }
                profileMap(rs)
            }
        }
    }

    Map<String, Object> linkProfileToIndex(String indexName, String profileName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_index_profile (semantic_index_id, embedding_profile_id)
                 SELECT i.id, p.id
                 FROM semantic.semantic_index i
                      CROSS JOIN semantic.embedding_profile p
                 WHERE i.name = ?
                   AND p.name = ?
                 ON CONFLICT DO NOTHING
             ''')) {
            statement.setString(1, indexName)
            statement.setString(2, profileName)
            statement.executeUpdate()
        }
        [
            indexName: indexName,
            profileName: profileName,
            linked: true
        ] as Map<String, Object>
    }

    Map<String, Object> unlinkProfileFromIndex(String indexName, String profileName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_index_profile ip
                 USING semantic.semantic_index i,
                       semantic.embedding_profile p
                 WHERE ip.semantic_index_id = i.id
                   AND ip.embedding_profile_id = p.id
                   AND i.name = ?
                   AND p.name = ?
             ''')) {
            statement.setString(1, indexName)
            statement.setString(2, profileName)
            deleted = statement.executeUpdate()
        }
        [
            indexName: indexName,
            profileName: profileName,
            linked: false,
            removed: deleted
        ] as Map<String, Object>
    }

    Map<String, Object> deleteEmbeddingsForIndex(String indexName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_embedding e
                 USING semantic.semantic_index i,
                       semantic.semantic_index_profile ip
                 WHERE ip.semantic_index_id = i.id
                   AND ip.embedding_profile_id = e.embedding_profile_id
                   AND i.name = ?
             ''')) {
            statement.setString(1, indexName)
            deleted = statement.executeUpdate()
        }
        [indexName: indexName, deletedEmbeddings: deleted] as Map<String, Object>
    }

    Map<String, Object> deleteChunksForCorpus(String corpusName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_chunk chunk
                 USING semantic.semantic_corpus corpus
                 WHERE chunk.corpus_id = corpus.id
                   AND corpus.name = ?
             ''')) {
            statement.setString(1, corpusName)
            deleted = statement.executeUpdate()
        }
        [corpusName: corpusName, deletedChunks: deleted] as Map<String, Object>
    }

    UUID corpusId(String corpusName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('SELECT id FROM semantic.semantic_corpus WHERE name = ?')) {
            statement.setString(1, corpusName ?: 'catalogue-items')
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() ? (UUID) rs.getObject(1) : null
            }
        }
    }

    boolean indexExists(String indexName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('SELECT EXISTS (SELECT 1 FROM semantic.semantic_index WHERE name = ?)')) {
            statement.setString(1, indexName)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getBoolean(1)
            }
        }
    }

    int countCatalogueCandidateChunks(String corpusName, List<String> domainTypes = [], UUID mauroModelId = null, Integer maxRows = null) {
        String sql = """
            WITH source_rows AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows)}
            ),
            chunks AS (
                SELECT btrim(label) AS source_text FROM source_rows WHERE label IS NOT NULL AND btrim(label) <> ''
                UNION ALL
                SELECT btrim(description) AS source_text FROM source_rows WHERE description IS NOT NULL AND btrim(description) <> ''
                UNION ALL
                SELECT concat_ws('. ', NULLIF(btrim(domain_type), ''), NULLIF(btrim(label), ''), NULLIF(btrim(description), '')) AS source_text
                FROM source_rows
                WHERE concat_ws('. ', NULLIF(btrim(domain_type), ''), NULLIF(btrim(label), ''), NULLIF(btrim(description), '')) <> ''
            )
            SELECT count(*) FROM chunks
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getInt(1)
            }
        }
    }

    int reconcileCatalogueChunks(String corpusName, List<String> domainTypes = [], UUID mauroModelId = null, Integer maxRows = null) {
        UUID corpusId = corpusId(corpusName ?: 'catalogue-items')
        if (corpusId == null) {
            return 0
        }
        String sql = """
            WITH source_rows AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows)}
            ),
            chunks AS (
                SELECT id, domain_type, label, model_id, 'label'::varchar AS chunk_kind, 0 AS chunk_ordinal, btrim(label) AS source_text, date_created, last_updated
                FROM source_rows
                WHERE label IS NOT NULL AND btrim(label) <> ''
                UNION ALL
                SELECT id, domain_type, label, model_id, 'description'::varchar AS chunk_kind, 1 AS chunk_ordinal, btrim(description) AS source_text, date_created, last_updated
                FROM source_rows
                WHERE description IS NOT NULL AND btrim(description) <> ''
                UNION ALL
                SELECT id, domain_type, label, model_id, 'summary'::varchar AS chunk_kind, 2 AS chunk_ordinal, concat_ws('. ', NULLIF(btrim(domain_type), ''), NULLIF(btrim(label), ''), NULLIF(btrim(description), '')) AS source_text, date_created, last_updated
                FROM source_rows
                WHERE concat_ws('. ', NULLIF(btrim(domain_type), ''), NULLIF(btrim(label), ''), NULLIF(btrim(description), '')) <> ''
            )
            INSERT INTO semantic.semantic_chunk (
                corpus_id, source_type, source_id, source_domain_type, source_label, mauro_model_id,
                chunk_kind, chunk_ordinal, source_text, content_hash, date_created, last_updated
            )
            SELECT ?::uuid,
                   'catalogue-item',
                   id,
                   domain_type,
                   label,
                   model_id,
                   chunk_kind,
                   chunk_ordinal,
                   btrim(source_text),
                   encode(sha256(convert_to(btrim(source_text), 'UTF8')), 'hex'),
                   date_created,
                   last_updated
            FROM chunks
            ON CONFLICT (corpus_id, source_type, source_id, chunk_kind, chunk_ordinal)
            DO UPDATE SET source_domain_type = EXCLUDED.source_domain_type,
                          source_label = EXCLUDED.source_label,
                          mauro_model_id = EXCLUDED.mauro_model_id,
                          source_text = EXCLUDED.source_text,
                          content_hash = EXCLUDED.content_hash,
                          date_created = EXCLUDED.date_created,
                          last_updated = EXCLUDED.last_updated,
                          indexed_at = now()
            WHERE semantic.semantic_chunk.source_domain_type IS DISTINCT FROM EXCLUDED.source_domain_type
               OR semantic.semantic_chunk.source_label IS DISTINCT FROM EXCLUDED.source_label
               OR semantic.semantic_chunk.mauro_model_id IS DISTINCT FROM EXCLUDED.mauro_model_id
               OR semantic.semantic_chunk.source_text IS DISTINCT FROM EXCLUDED.source_text
               OR semantic.semantic_chunk.content_hash IS DISTINCT FROM EXCLUDED.content_hash
               OR semantic.semantic_chunk.date_created IS DISTINCT FROM EXCLUDED.date_created
               OR semantic.semantic_chunk.last_updated IS DISTINCT FROM EXCLUDED.last_updated
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setObject(index, corpusId)
            statement.executeUpdate()
        }
    }

    int countChunksNeedingEmbedding(EmbeddingProfile profile,
                                    String corpusName,
                                    List<String> domainTypes = [],
                                    UUID mauroModelId = null,
                                    Integer maxRows = null,
                                    boolean force = false) {
        String filterClause = chunkSourceFilterClause(domainTypes, mauroModelId, maxRows)
        String embeddingClause = force ? '' : '''
              AND NOT EXISTS (
                  SELECT 1
                  FROM semantic.semantic_embedding e
                  WHERE e.chunk_id = c.id
                    AND e.embedding_profile_id = ?
                    AND e.content_hash = c.content_hash
              )
        '''
        String sql = """
            WITH selected_sources AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows, true)}
            )
            SELECT count(*)
            FROM semantic.semantic_chunk c
                 JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                 ${filterClause}
            WHERE corpus.name = ?
              ${embeddingClause}
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (!force) {
                statement.setObject(index, profile.id)
            }
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getInt(1)
            }
        }
    }

    List<SemanticChunk> nextChunksNeedingEmbedding(EmbeddingProfile profile,
                                                   String corpusName,
                                                   List<String> domainTypes = [],
                                                   UUID mauroModelId = null,
                                                   Integer maxRows = null,
                                                   boolean force = false,
                                                   int limit = 512,
                                                   UUID afterChunkId = null) {
        String filterClause = chunkSourceFilterClause(domainTypes, mauroModelId, maxRows)
        String keysetClause = afterChunkId == null ? '' : ' AND c.id > ?'
        String embeddingClause = force ? '' : '''
              AND NOT EXISTS (
                  SELECT 1
                  FROM semantic.semantic_embedding e
                  WHERE e.chunk_id = c.id
                    AND e.embedding_profile_id = ?
                    AND e.content_hash = c.content_hash
              )
        '''
        String sql = """
            WITH selected_sources AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows, true)}
            )
            SELECT c.id,
                   c.corpus_id,
                   c.source_type,
                   c.source_id,
                   c.source_domain_type,
                   c.source_label,
                   c.mauro_model_id,
                   c.chunk_kind,
                   c.chunk_ordinal,
                   c.source_text,
                   c.content_hash,
                   c.date_created,
                   c.last_updated
            FROM semantic.semantic_chunk c
                 JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                 ${filterClause}
            WHERE corpus.name = ?
              ${keysetClause}
              ${embeddingClause}
            ORDER BY c.id
            LIMIT ?
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (afterChunkId != null) {
                statement.setObject(index++, afterChunkId)
            }
            if (!force) {
                statement.setObject(index++, profile.id)
            }
            statement.setInt(index, Math.max(limit, 1))
            try (ResultSet rs = statement.executeQuery()) {
                List<SemanticChunk> chunks = new ArrayList<SemanticChunk>()
                while (rs.next()) {
                    chunks.add(chunkFrom(rs))
                }
                chunks
            }
        }
    }

    List<SemanticChunk> catalogueChunks(String corpusName, List<String> domainTypes = [], UUID mauroModelId = null, Integer maxRows = null) {
        UUID corpusId = corpusId(corpusName ?: 'catalogue-items')
        if (corpusId == null) {
            return Collections.<SemanticChunk>emptyList()
        }
        String domainClause = domainTypes == null || domainTypes.isEmpty() ? '' : ' AND sd.domain_type = ANY (?::varchar[])'
        String mauroModelClause = mauroModelId == null ? '' : ' AND sd.model_id = ?'
        String limitClause = maxRows == null ? '' : ' LIMIT ?'
        String sql = """
            SELECT sd.id, sd.domain_type, sd.label, sd.description, sd.date_created, sd.last_updated, sd.model_id
            FROM search.search_domains sd
            WHERE 1 = 1 ${domainClause} ${mauroModelClause}
            ORDER BY sd.domain_type, sd.label
            ${limitClause}
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1
            if (domainTypes != null && !domainTypes.isEmpty()) {
                statement.setArray(index++, connection.createArrayOf('varchar', domainTypes.toArray(new String[0])))
            }
            if (mauroModelId != null) {
                statement.setObject(index++, mauroModelId)
            }
            if (maxRows != null) {
                statement.setInt(index, Math.max(1, maxRows))
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<SemanticChunk> chunks = new ArrayList<SemanticChunk>()
                while (rs.next()) {
                    chunks.addAll(chunksForSearchDomainRow(corpusId, rs))
                }
                chunks
            }
        }
    }

    UUID upsertChunk(SemanticChunk chunk) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_chunk (
                     corpus_id, source_type, source_id, source_domain_type, source_label, mauro_model_id,
                     chunk_kind, chunk_ordinal, source_text, content_hash, date_created, last_updated
                 )
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT (corpus_id, source_type, source_id, chunk_kind, chunk_ordinal)
                 DO UPDATE SET source_domain_type = EXCLUDED.source_domain_type,
                               source_label = EXCLUDED.source_label,
                               mauro_model_id = EXCLUDED.mauro_model_id,
                               source_text = EXCLUDED.source_text,
                               content_hash = EXCLUDED.content_hash,
                               date_created = EXCLUDED.date_created,
                               last_updated = EXCLUDED.last_updated,
                               indexed_at = now()
                 RETURNING id
             ''')) {
            statement.setObject(1, chunk.corpusId)
            statement.setString(2, chunk.sourceType)
            statement.setObject(3, chunk.sourceId)
            statement.setString(4, chunk.sourceDomainType)
            statement.setString(5, chunk.sourceLabel)
            statement.setObject(6, chunk.mauroModelId)
            statement.setString(7, chunk.chunkKind)
            statement.setInt(8, chunk.chunkOrdinal ?: 0)
            statement.setString(9, chunk.sourceText)
            statement.setString(10, chunk.contentHash)
            setInstant(statement, 11, chunk.dateCreated)
            setInstant(statement, 12, chunk.lastUpdated)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                (UUID) rs.getObject(1)
            }
        }
    }

    List<SemanticChunk> chunksNeedingEmbedding(EmbeddingProfile profile, List<SemanticChunk> chunks, int batchSize = 1000) {
        if (profile == null || chunks == null || chunks.isEmpty()) {
            return Collections.<SemanticChunk>emptyList()
        }
        Map<UUID, SemanticChunk> chunksById = chunks.findAll {SemanticChunk chunk -> chunk.id != null}
            .collectEntries {SemanticChunk chunk -> [(chunk.id): chunk]} as Map<UUID, SemanticChunk>
        Set<UUID> currentChunkIds = new LinkedHashSet<UUID>()
        for (List<SemanticChunk> batch : chunksById.values().toList().collate(Math.max(batchSize, 1))) {
            try (Connection connection = dataSource.connection;
                 PreparedStatement statement = connection.prepareStatement('''
                     SELECT e.chunk_id
                     FROM semantic.semantic_embedding e
                     WHERE e.embedding_profile_id = ?
                       AND e.chunk_id = ANY (?::uuid[])
                 ''')) {
                statement.setObject(1, profile.id)
                statement.setArray(2, connection.createArrayOf('uuid', batch.collect {SemanticChunk chunk -> chunk.id}.toArray()))
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        UUID chunkId = (UUID) rs.getObject('chunk_id')
                        SemanticChunk chunk = chunksById.get(chunkId)
                        if (chunk != null) {
                            currentChunkIds.add(chunkId)
                        }
                    }
                }
            }
        }

        chunks.findAll {SemanticChunk chunk ->
            chunk.id == null || !currentChunkIds.contains(chunk.id)
        } as List<SemanticChunk>
    }

    int deleteStaleEmbeddings(EmbeddingProfile profile) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_embedding e
                 USING semantic.semantic_chunk c
                 WHERE e.chunk_id = c.id
                   AND e.embedding_profile_id = ?
                   AND e.content_hash <> c.content_hash
             ''')) {
            statement.setObject(1, profile.id)
            statement.executeUpdate()
        }
    }

    void updateIndexStatus(String indexName, String status) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.semantic_index
                 SET status = ?,
                     last_indexed_at = CASE WHEN ? = 'READY' THEN now() ELSE last_indexed_at END,
                     updated_at = now()
                 WHERE name = ?
             ''')) {
            statement.setString(1, status)
            statement.setString(2, status)
            statement.setString(3, indexName)
            statement.executeUpdate()
        }
    }

    void upsertEmbeddings(List<SemanticChunk> chunks, EmbeddingProfile profile, List<float[]> embeddings) {
        if (chunks == null || chunks.isEmpty()) {
            return
        }
        if (embeddings == null || embeddings.size() != chunks.size()) {
            throw new IllegalArgumentException("Expected ${chunks.size()} embeddings but received ${embeddings == null ? 0 : embeddings.size()}")
        }
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_embedding (chunk_id, embedding_profile_id, content_hash, embedding)
                 VALUES (?, ?, ?, ?::vector)
                 ON CONFLICT (chunk_id, embedding_profile_id)
                 DO UPDATE SET content_hash = EXCLUDED.content_hash,
                               embedding = EXCLUDED.embedding,
                               updated_at = now()
             ''')) {
            for (int i = 0; i < chunks.size(); i++) {
                SemanticChunk chunk = chunks.get(i)
                statement.setObject(1, chunk.id)
                statement.setObject(2, profile.id)
                statement.setString(3, chunk.contentHash)
                statement.setString(4, vectorLiteral(embeddings.get(i)))
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    void dropVectorIndex(EmbeddingProfile profile) {
        executeIndexStatement("DROP INDEX IF EXISTS semantic.${vectorIndexName(profile)}")
    }

    void createVectorIndex(EmbeddingProfile profile) {
        String vectorCast = "vector(${Math.max(profile.dimension ?: 0, 1)})"
        String operatorClass = vectorOperatorClass(profile)
        String profileId = profile.id.toString().replace("'", "''")
        executeIndexStatement("""
            CREATE INDEX IF NOT EXISTS ${vectorIndexName(profile)}
            ON semantic.semantic_embedding
            USING hnsw ((embedding::${vectorCast}) ${operatorClass})
            WHERE embedding_profile_id = '${profileId}'
        """)
    }

    boolean vectorIndexExists(EmbeddingProfile profile) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('SELECT to_regclass(?) IS NOT NULL')) {
            statement.setString(1, "semantic.${vectorIndexName(profile)}")
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getBoolean(1)
            }
        }
    }

    long countEmbeddingsForProfile(EmbeddingProfile profile) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT count(*)
                 FROM semantic.semantic_embedding
                 WHERE embedding_profile_id = ?
             ''')) {
            statement.setObject(1, profile.id)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getLong(1)
            }
        }
    }

    boolean hasEmbeddings(String indexName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT EXISTS (
                     SELECT 1
                     FROM semantic.semantic_embedding e
                          JOIN semantic.semantic_index_profile ip ON ip.embedding_profile_id = e.embedding_profile_id
                          JOIN semantic.semantic_index i ON i.id = ip.semantic_index_id
                     WHERE i.name = ?
                 )
             ''')) {
            statement.setString(1, indexName ?: 'catalogue-items-default')
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                rs.getBoolean(1)
            }
        }
    }

    List<SemanticCandidate> search(EmbeddingProfile profile,
                                   float[] queryEmbedding,
                                   String corpusName,
                                   List<String> domainTypes,
                                   UUID mauroModelId,
                                   int topN) {
        String domainClause = domainTypes == null || domainTypes.isEmpty() ? '' : ' AND c.source_domain_type = ANY (?::varchar[])'
        String mauroModelClause = mauroModelId == null ? '' : ' AND c.mauro_model_id = ?'
        String vectorCast = "vector(${Math.max(profile.dimension ?: 0, 1)})"
        String distanceExpression = "(e.embedding::${vectorCast} <=> ?::${vectorCast})"
        String sql = """
            SELECT c.id AS chunk_id,
                   c.source_id,
                   c.source_domain_type,
                   c.source_label,
                   sd.description,
                   c.chunk_kind,
                   c.chunk_ordinal,
                   c.source_text,
                   p.name AS embedding_profile,
                   ${distanceExpression} AS distance,
                   c.date_created,
                   c.last_updated
            FROM semantic.semantic_embedding e
                 JOIN semantic.semantic_chunk c ON c.id = e.chunk_id
                 JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                 JOIN semantic.embedding_profile p ON p.id = e.embedding_profile_id
                 LEFT JOIN search.search_domains sd ON sd.id = c.source_id AND sd.domain_type = c.source_domain_type
            WHERE p.id = ?
              AND corpus.name = ?
              ${domainClause}
              ${mauroModelClause}
            ORDER BY e.embedding::${vectorCast} <=> ?::${vectorCast}
            LIMIT ?
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1
            String queryVector = vectorLiteral(queryEmbedding)
            statement.setString(index++, queryVector)
            statement.setObject(index++, profile.id)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (domainTypes != null && !domainTypes.isEmpty()) {
                statement.setArray(index++, connection.createArrayOf('varchar', domainTypes.toArray(new String[0])))
            }
            if (mauroModelId != null) {
                statement.setObject(index++, mauroModelId)
            }
            statement.setString(index++, queryVector)
            statement.setInt(index, Math.max(1, topN))
            try (ResultSet rs = statement.executeQuery()) {
                List<SemanticCandidate> candidates = new ArrayList<SemanticCandidate>()
                while (rs.next()) {
                    candidates.add(candidateFrom(rs))
                }
                candidates
            }
        }
    }

    private static EmbeddingProfile profileFrom(ResultSet rs) {
        new EmbeddingProfile(
            id: (UUID) rs.getObject('id'),
            name: rs.getString('name'),
            provider: rs.getString('provider'),
            embeddingModel: rs.getString('embedding_model'),
            dimension: rs.getInt('dimension'),
            distanceMetric: rs.getString('distance_metric'),
            description: hasColumn(rs, 'description') ? rs.getString('description') : null
        )
    }

    private static Map<String, Object> profileMap(ResultSet rs) {
        [
            id: String.valueOf(rs.getObject('id')),
            name: rs.getString('name'),
            provider: rs.getString('provider'),
            embeddingModel: rs.getString('embedding_model'),
            dimension: rs.getInt('dimension'),
            distanceMetric: rs.getString('distance_metric'),
            enabled: rs.getBoolean('enabled'),
            description: rs.getString('description')
        ] as Map<String, Object>
    }

    private static List<String> arrayToList(Object array) {
        if (array instanceof Object[]) {
            return ((Object[]) array).collect {Object item -> String.valueOf(item)} as List<String>
        }
        Collections.<String>emptyList()
    }

    private static String requiredString(Map<String, Object> request, String key) {
        String value = stringValue(request, key)
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required field ${key}")
        }
        value.trim()
    }

    private static String stringValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        value == null ? null : String.valueOf(value)
    }

    private static Integer requiredInteger(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        if (value == null) {
            throw new IllegalArgumentException("Missing required field ${key}")
        }
        value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(String.valueOf(value))
    }

    private static boolean booleanValue(Map<String, Object> request, String key, boolean fallback) {
        Object value = request == null ? null : request.get(key)
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }

    private static boolean hasColumn(ResultSet rs, String columnName) {
        for (int i = 1; i <= rs.metaData.columnCount; i++) {
            if (rs.metaData.getColumnLabel(i).equalsIgnoreCase(columnName)) {
                return true
            }
        }
        false
    }

    private static SemanticCandidate candidateFrom(ResultSet rs) {
        double distance = rs.getDouble('distance')
        new SemanticCandidate(
            chunkId: (UUID) rs.getObject('chunk_id'),
            sourceId: (UUID) rs.getObject('source_id'),
            sourceDomainType: rs.getString('source_domain_type'),
            sourceLabel: rs.getString('source_label'),
            description: rs.getString('description'),
            chunkKind: rs.getString('chunk_kind'),
            chunkOrdinal: rs.getInt('chunk_ordinal'),
            sourceText: rs.getString('source_text'),
            embeddingProfile: rs.getString('embedding_profile'),
            distance: distance,
            similarity: 1D - distance,
            dateCreated: instant(rs, 'date_created'),
            lastUpdated: instant(rs, 'last_updated')
        )
    }

    private static List<SemanticChunk> chunksForSearchDomainRow(UUID corpusId, ResultSet rs) {
        UUID sourceId = (UUID) rs.getObject('id')
        String domainType = rs.getString('domain_type')
        String label = rs.getString('label')
        String description = rs.getString('description')
        UUID mauroModelId = (UUID) rs.getObject('model_id')
        Instant dateCreated = instant(rs, 'date_created')
        Instant lastUpdated = instant(rs, 'last_updated')
        List<SemanticChunk> chunks = new ArrayList<SemanticChunk>()
        int ordinal = 0
        addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'label', ordinal++, label, dateCreated, lastUpdated)
        addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'description', ordinal++, description, dateCreated, lastUpdated)
        addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'summary', ordinal, [domainType, label, description].findAll {String value -> value}.join('. '), dateCreated, lastUpdated)
        chunks
    }

    private static void addChunk(List<SemanticChunk> chunks,
                                 UUID corpusId,
                                 UUID sourceId,
                                 String domainType,
                                 String label,
                                 UUID mauroModelId,
                                 String kind,
                                 int ordinal,
                                 String text,
                                 Instant dateCreated,
                                 Instant lastUpdated) {
        if (text == null || text.trim().isEmpty()) {
            return
        }
        String clean = text.trim()
        chunks.add(new SemanticChunk(
            corpusId: corpusId,
            sourceType: 'catalogue-item',
            sourceId: sourceId,
            sourceDomainType: domainType,
            sourceLabel: label,
            mauroModelId: mauroModelId,
            chunkKind: kind,
            chunkOrdinal: ordinal,
            sourceText: clean,
            contentHash: sha256(clean),
            dateCreated: dateCreated,
            lastUpdated: lastUpdated
        ))
    }

    private static String vectorLiteral(float[] vector) {
        '[' + vector.collect {float value -> Float.toString(value)}.join(',') + ']'
    }

    private static String catalogueSourceRowsSql(List<String> domainTypes, UUID mauroModelId, Integer maxRows, boolean identifiersOnly = false) {
        String domainClause = domainTypes == null || domainTypes.isEmpty() ? '' : ' AND sd.domain_type = ANY (?::varchar[])'
        String mauroModelClause = mauroModelId == null ? '' : ' AND sd.model_id = ?'
        String limitClause = maxRows == null ? '' : ' LIMIT ?'
        String selectColumns = identifiersOnly ?
            'sd.id, sd.domain_type' :
            'sd.id, sd.domain_type, sd.label, sd.description, sd.date_created, sd.last_updated, sd.model_id'
        """
            SELECT ${selectColumns}
            FROM search.search_domains sd
            WHERE 1 = 1 ${domainClause} ${mauroModelClause}
            ORDER BY sd.domain_type, sd.label
            ${limitClause}
        """
    }

    private static int bindCatalogueSourceRows(PreparedStatement statement,
                                               Connection connection,
                                               List<String> domainTypes,
                                               UUID mauroModelId,
                                               Integer maxRows,
                                               int index = 1) {
        if (domainTypes != null && !domainTypes.isEmpty()) {
            statement.setArray(index++, connection.createArrayOf('varchar', domainTypes.toArray(new String[0])))
        }
        if (mauroModelId != null) {
            statement.setObject(index++, mauroModelId)
        }
        if (maxRows != null) {
            statement.setInt(index++, Math.max(1, maxRows))
        }
        index
    }

    private static String chunkSourceFilterClause(List<String> domainTypes, UUID mauroModelId, Integer maxRows) {
        if ((domainTypes == null || domainTypes.isEmpty()) && mauroModelId == null && maxRows == null) {
            return ''
        }
        'JOIN selected_sources selected ON selected.id = c.source_id AND selected.domain_type = c.source_domain_type'
    }

    private static SemanticChunk chunkFrom(ResultSet rs) {
        new SemanticChunk(
            id: (UUID) rs.getObject('id'),
            corpusId: (UUID) rs.getObject('corpus_id'),
            sourceType: rs.getString('source_type'),
            sourceId: (UUID) rs.getObject('source_id'),
            sourceDomainType: rs.getString('source_domain_type'),
            sourceLabel: rs.getString('source_label'),
            mauroModelId: (UUID) rs.getObject('mauro_model_id'),
            chunkKind: rs.getString('chunk_kind'),
            chunkOrdinal: rs.getInt('chunk_ordinal'),
            sourceText: rs.getString('source_text'),
            contentHash: rs.getString('content_hash'),
            dateCreated: instant(rs, 'date_created'),
            lastUpdated: instant(rs, 'last_updated')
        )
    }

    private void executeIndexStatement(String sql) {
        try (Connection connection = dataSource.connection;
             java.sql.Statement statement = connection.createStatement()) {
            statement.execute(sql)
        }
    }

    private static String vectorIndexName(EmbeddingProfile profile) {
        String safeName = (profile.name ?: 'profile')
            .replaceAll('[^A-Za-z0-9]+', '_')
            .replaceAll('^_+', '')
            .replaceAll('_+$', '')
            .toLowerCase()
        if (safeName.length() > 38) {
            safeName = safeName.substring(0, 38)
        }
        "semantic_embedding_${safeName}_hnsw_idx"
    }

    private static String vectorOperatorClass(EmbeddingProfile profile) {
        switch (profile.distanceMetric) {
            case 'l2':
                return 'vector_l2_ops'
            case 'inner_product':
                return 'vector_ip_ops'
            case 'cosine':
            default:
                return 'vector_cosine_ops'
        }
    }

    private static String sha256(String text) {
        MessageDigest digest = MessageDigest.getInstance('SHA-256')
        byte[] hash = digest.digest(text.getBytes('UTF-8'))
        StringBuilder builder = new StringBuilder(hash.length * 2)
        for (byte b : hash) {
            builder.append(String.format('%02x', b & 0xff))
        }
        builder.toString()
    }

    private static void setInstant(PreparedStatement statement, int index, Instant instant) {
        if (instant == null) {
            statement.setTimestamp(index, null)
        } else {
            statement.setTimestamp(index, Timestamp.from(instant))
        }
    }

    private static Instant instant(ResultSet rs, String column) {
        Timestamp timestamp = rs.getTimestamp(column)
        timestamp == null ? null : timestamp.toInstant()
    }
}
