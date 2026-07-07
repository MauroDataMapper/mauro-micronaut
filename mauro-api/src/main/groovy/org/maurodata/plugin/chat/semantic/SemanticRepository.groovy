package org.maurodata.plugin.chat.semantic

import org.maurodata.service.chat.semantic.*
import org.maurodata.service.search.*
import org.maurodata.service.semantic.*

import com.zaxxer.hikari.HikariDataSource
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.annotation.Value
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Singleton
import org.maurodata.domain.search.dto.SearchResultsDTO

import javax.sql.DataSource
import java.security.MessageDigest
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.sql.Timestamp
import java.time.Instant

@CompileStatic
@Singleton
@Slf4j
@Connectable
class SemanticRepository {

    private final DataSource dataSource
    private final int hnswCatalogueEfSearch
    private final int hnswContextEfSearch

    SemanticRepository(DataSource dataSource,
                       @Value('${chat.semantic.search.hnsw-ef-search:10}') Integer hnswEfSearch,
                       @Value('${chat.semantic.search.catalogue-hnsw-ef-search:10}') Integer hnswCatalogueEfSearch,
                       @Value('${chat.semantic.search.context-hnsw-ef-search:40}') Integer hnswContextEfSearch) {
        this.dataSource = dataSource
        int fallback = Math.max(hnswEfSearch ?: 10, 1)
        this.hnswCatalogueEfSearch = Math.max(hnswCatalogueEfSearch ?: fallback, 1)
        this.hnswContextEfSearch = Math.max(hnswContextEfSearch ?: Math.max(fallback, 40), 1)
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

    List<Map<String, Object>> corpora(boolean includeInternal = false) {
        String visibilityClause = includeInternal ? '' : 'WHERE api_visible = TRUE'
        String sql = """
            SELECT id,
                   name,
                   source,
                   description,
                   enabled,
                   origin,
                   api_visible,
                   api_manageable,
                   chunk_delete_api_enabled,
                   created_at,
                   updated_at
            FROM semantic.semantic_corpus
            ${visibilityClause}
            ORDER BY name
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    rows.add(corpusMap(rs))
                }
                rows
            }
        }
    }

    Map<String, Object> createCorpus(Map<String, Object> request) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_corpus (
                     name,
                     source,
                     description,
                     enabled,
                     origin,
                     api_visible,
                     api_manageable,
                     chunk_delete_api_enabled
                 )
                 VALUES (?, ?, ?, ?, 'api', TRUE, TRUE, ?)
                 ON CONFLICT (name) DO UPDATE
                 SET source = EXCLUDED.source,
                     description = EXCLUDED.description,
                     enabled = EXCLUDED.enabled,
                     updated_at = now()
                 WHERE semantic_corpus.api_manageable = TRUE
                 RETURNING id,
                           name,
                           source,
                           description,
                           enabled,
                           origin,
                           api_visible,
                           api_manageable,
                           chunk_delete_api_enabled,
                           created_at,
                           updated_at
             ''')) {
            statement.setString(1, requiredString(request, 'name'))
            statement.setString(2, stringValue(request, 'source') ?: 'api')
            statement.setString(3, stringValue(request, 'description'))
            statement.setBoolean(4, booleanValue(request, 'enabled', true))
            statement.setBoolean(5, booleanValue(request, 'chunkDeleteApiEnabled', false))
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("Semantic corpus ${stringValue(request, 'name')} is not API manageable")
                }
                corpusMap(rs)
            }
        }
    }

    List<String> apiCorpusNames() {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT name
                 FROM semantic.semantic_corpus
                 WHERE enabled = TRUE
                   AND api_visible = TRUE
                 ORDER BY name
             ''')) {
            try (ResultSet rs = statement.executeQuery()) {
                List<String> names = new ArrayList<String>()
                while (rs.next()) {
                    names.add(rs.getString('name'))
                }
                names
            }
        }
    }

    boolean apiCorpusVisible(String corpusName) {
        if (corpusName == null || corpusName.trim().isEmpty()) {
            return false
        }
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT enabled = TRUE AND api_visible = TRUE
                 FROM semantic.semantic_corpus
                 WHERE name = ?
             ''')) {
            statement.setString(1, corpusName)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() && rs.getBoolean(1)
            }
        }
    }

    List<String> apiCorpusNamesForModelIndex(UUID mauroModelId, String requestedCorpusName = null) {
        if (mauroModelId == null) {
            return requestedCorpusName == null || requestedCorpusName.trim().isEmpty() ?
                apiCorpusNames() :
                (apiCorpusVisible(requestedCorpusName) ? [requestedCorpusName] as List<String> : Collections.<String>emptyList())
        }
        String corpusClause = requestedCorpusName == null || requestedCorpusName.trim().isEmpty() ? '' : 'AND c.name = ?'
        String sql = """
            SELECT DISTINCT c.name
            FROM semantic.semantic_model_index mi
                 JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
            WHERE mi.mauro_model_id = ?
              AND mi.enabled = TRUE
              AND mi.status = 'READY'
              AND c.enabled = TRUE
              AND c.api_visible = TRUE
              ${corpusClause}
            ORDER BY c.name
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, mauroModelId)
            if (requestedCorpusName != null && !requestedCorpusName.trim().isEmpty()) {
                statement.setString(2, requestedCorpusName)
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<String> names = new ArrayList<String>()
                while (rs.next()) {
                    names.add(rs.getString('name'))
                }
                names
            }
        }
    }

    List<EmbeddingProfile> profilesForCorpora(List<String> corpusNames) {
        if (corpusNames == null || corpusNames.isEmpty()) {
            return Collections.<EmbeddingProfile>emptyList()
        }
        String placeholders = corpusNames.collect {'?'}.join(',')
        String sql = """
            SELECT DISTINCT profile.id,
                            profile.name,
                            profile.provider,
                            profile.embedding_model,
                            profile.dimension,
                            profile.distance_metric,
                            profile.description
            FROM (
                SELECT p.id, p.name, p.provider, p.embedding_model, p.dimension, p.distance_metric, p.description
                FROM semantic.embedding_profile p
                     JOIN semantic.semantic_index_profile ip ON ip.embedding_profile_id = p.id
                     JOIN semantic.semantic_index i ON i.id = ip.semantic_index_id
                     JOIN semantic.semantic_corpus c ON c.id = i.corpus_id
                WHERE p.enabled = TRUE
                  AND c.enabled = TRUE
                  AND c.api_visible = TRUE
                  AND c.name IN (${placeholders})
                UNION
                SELECT p.id, p.name, p.provider, p.embedding_model, p.dimension, p.distance_metric, p.description
                FROM semantic.embedding_profile p
                     JOIN semantic.semantic_model_index mi ON mi.embedding_profile_id = p.id
                     JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                WHERE p.enabled = TRUE
                  AND mi.enabled = TRUE
                  AND mi.status = 'READY'
                  AND c.enabled = TRUE
                  AND c.api_visible = TRUE
                  AND c.name IN (${placeholders})
            ) profile
            ORDER BY profile.name
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < corpusNames.size(); i++) {
                statement.setString(i + 1, corpusNames.get(i))
            }
            for (int i = 0; i < corpusNames.size(); i++) {
                statement.setString(corpusNames.size() + i + 1, corpusNames.get(i))
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<EmbeddingProfile> profiles = new ArrayList<EmbeddingProfile>()
                while (rs.next()) {
                    profiles.add(profileFrom(rs))
                }
                profiles
            }
        }
    }

    Map<String, Object> indexingStatus() {
        Map<String, Object> global = indexingControl('global')
        Map<String, Object> autoReconcile = indexingControl('auto-reconcile')
        [
            enabled: Boolean.TRUE.equals(global.get('enabled')),
            updatedAt: global.get('updatedAt'),
            autoReconcile: Boolean.TRUE.equals(autoReconcile.get('enabled')),
            autoReconcileUpdatedAt: autoReconcile.get('updatedAt')
        ] as Map<String, Object>
    }

    private Map<String, Object> indexingControl(String name) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT enabled, updated_at
                 FROM semantic.semantic_indexing_control
                 WHERE name = ?
             ''')) {
            statement.setString(1, name)
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return [enabled: false] as Map<String, Object>
                }
                [
                    enabled: rs.getBoolean('enabled'),
                    updatedAt: rs.getTimestamp('updated_at')?.toInstant()?.toString()
                ] as Map<String, Object>
            }
        }
    }

    boolean indexingEnabled() {
        Boolean.TRUE.equals(indexingStatus().get('enabled'))
    }

    boolean autoReconcileEnabled() {
        Boolean.TRUE.equals(indexingStatus().get('autoReconcile'))
    }

    Map<String, Object> setIndexingEnabled(boolean enabled) {
        Map<String, Object> control = setIndexingControl('global', enabled)
        Map<String, Object> status = indexingStatus()
        status.put('changedControl', control)
        status
    }

    Map<String, Object> setAutoReconcileEnabled(boolean enabled) {
        Map<String, Object> control = setIndexingControl('auto-reconcile', enabled)
        Map<String, Object> status = indexingStatus()
        status.put('changedControl', control)
        status
    }

    private Map<String, Object> setIndexingControl(String name, boolean enabled) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_indexing_control (name, enabled, updated_at)
                 VALUES (?, ?, now())
                 ON CONFLICT (name) DO UPDATE
                 SET enabled = EXCLUDED.enabled,
                     updated_at = now()
                 RETURNING name, enabled, updated_at
             ''')) {
            statement.setString(1, name)
            statement.setBoolean(2, enabled)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                [
                    name: rs.getString('name'),
                    enabled: rs.getBoolean('enabled'),
                    updatedAt: rs.getTimestamp('updated_at')?.toInstant()?.toString()
                ] as Map<String, Object>
            }
        }
    }

    List<Map<String, Object>> cancelActiveJobs(String reason) {
        List<Map<String, Object>> activeJobs = jobs(['QUEUED', 'TO_RESTART', 'RUNNING'] as List<String>)
        for (Map<String, Object> job : activeJobs) {
            cancelJob(UUID.fromString(String.valueOf(job.get('jobId'))), reason ?: 'semantic indexing was disabled')
        }
        activeJobs
    }

    Map<String, Object> cancelJob(UUID jobId, String reason) {
        Map<String, Object> existingJob = job(jobId)
        String status = String.valueOf(existingJob.get('status'))
        if (['SUCCEEDED', 'FAILED', 'CANCELLED'].contains(status)) {
            return existingJob
        }
        updateJobStatus(
            jobId,
            'CANCELLED',
            [
                stage: 'cancelled',
                previousStatus: status,
                reason: reason ?: 'semantic indexing job was cancelled',
                cancelledAt: Instant.now().toString()
            ] as Map<String, Object>,
            null
        )
        job(jobId)
    }

    List<Map<String, Object>> modelIndexes() {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT mi.id,
                        mi.mauro_model_id,
                        COALESCE(model_label.label, mi.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        mi.enabled,
                        mi.status,
                        mi.last_error,
                        c.name AS corpus_name,
                        p.name AS profile_name
                 FROM semantic.semantic_model_index mi
                      JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = mi.embedding_profile_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = mi.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 ORDER BY mi.updated_at DESC, mi.created_at DESC
             ''')) {
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    rows.add(modelIndexSummaryMap(rs))
                }
                rows
            }
        }
    }

    List<Map<String, Object>> modelIndexStats(UUID mauroModelId) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT mi.id,
                        mi.mauro_model_id,
                        COALESCE(model_label.label, mi.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        mi.enabled,
                        mi.status,
                        mi.last_indexed_at,
                        mi.stale_requested_at,
                        mi.indexing_started_at,
                        mi.last_checked_at,
                        mi.last_error,
                        mi.created_at,
                        mi.updated_at,
                        c.name AS corpus_name,
                        p.name AS profile_name,
                        p.provider,
                        p.embedding_model,
                        p.dimension,
                        (SELECT count(*)
                         FROM semantic.semantic_chunk chunk
                         WHERE chunk.corpus_id = mi.corpus_id
                           AND chunk.mauro_model_id IN (
                               WITH RECURSIVE requested_scope(id) AS (
                                   SELECT mi.mauro_model_id
                               ),
                               scoped_folders(id) AS (
                                   SELECT folder.id
                                   FROM core.folder folder
                                        JOIN requested_scope scope ON scope.id = folder.id
                                   UNION ALL
                                   SELECT child.id
                                   FROM core.folder child
                                        JOIN scoped_folders parent ON child.parent_folder_id = parent.id
                               ),
                               scoped_model_ids(id) AS (
                                   SELECT scope.id
                                   FROM requested_scope scope
                                   WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
                                      OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
                                      OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
                                   UNION
                                   SELECT data_model.id
                                   FROM datamodel.data_model data_model
                                        JOIN scoped_folders folder ON folder.id = data_model.folder_id
                                   UNION
                                   SELECT terminology.id
                                   FROM terminology.terminology terminology
                                        JOIN scoped_folders folder ON folder.id = terminology.folder_id
                                   UNION
                                   SELECT code_set.id
                                   FROM terminology.code_set code_set
                                        JOIN scoped_folders folder ON folder.id = code_set.folder_id
                               )
                               SELECT id FROM scoped_model_ids
                           )) AS chunks,
                        (SELECT count(*)
                         FROM semantic.semantic_embedding embedding
                              JOIN semantic.semantic_chunk chunk ON chunk.id = embedding.chunk_id
                         WHERE chunk.corpus_id = mi.corpus_id
                           AND embedding.embedding_profile_id = mi.embedding_profile_id
                           AND chunk.mauro_model_id IN (
                               WITH RECURSIVE requested_scope(id) AS (
                                   SELECT mi.mauro_model_id
                               ),
                               scoped_folders(id) AS (
                                   SELECT folder.id
                                   FROM core.folder folder
                                        JOIN requested_scope scope ON scope.id = folder.id
                                   UNION ALL
                                   SELECT child.id
                                   FROM core.folder child
                                        JOIN scoped_folders parent ON child.parent_folder_id = parent.id
                               ),
                               scoped_model_ids(id) AS (
                                   SELECT scope.id
                                   FROM requested_scope scope
                                   WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
                                      OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
                                      OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
                                   UNION
                                   SELECT data_model.id
                                   FROM datamodel.data_model data_model
                                        JOIN scoped_folders folder ON folder.id = data_model.folder_id
                                   UNION
                                   SELECT terminology.id
                                   FROM terminology.terminology terminology
                                        JOIN scoped_folders folder ON folder.id = terminology.folder_id
                                   UNION
                                   SELECT code_set.id
                                   FROM terminology.code_set code_set
                                        JOIN scoped_folders folder ON folder.id = code_set.folder_id
                               )
                               SELECT id FROM scoped_model_ids
                           )) AS embeddings
                 FROM semantic.semantic_model_index mi
                      JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = mi.embedding_profile_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = mi.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 WHERE mi.mauro_model_id = ?
                 ORDER BY mi.updated_at DESC, mi.created_at DESC
             ''')) {
            statement.setObject(1, mauroModelId)
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    rows.add(modelIndexMap(rs))
                }
                rows
            }
        }
    }

    Map<String, Object> createModelIndex(UUID mauroModelId, String profileName, String corpusName, boolean enabled, String label = null) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_model_index (corpus_id, mauro_model_id, embedding_profile_id, enabled, status, label)
                 SELECT c.id, ?::uuid, p.id, ?, 'STALE', ?
                 FROM semantic.semantic_corpus c
                      CROSS JOIN semantic.embedding_profile p
                 WHERE c.name = ?
                   AND p.name = ?
                 ON CONFLICT (corpus_id, mauro_model_id, embedding_profile_id) DO UPDATE
                 SET enabled = EXCLUDED.enabled,
                     label = COALESCE(EXCLUDED.label, semantic_model_index.label),
                     updated_at = now()
                 RETURNING id
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setBoolean(2, enabled)
            statement.setString(3, label)
            statement.setString(4, corpusName ?: 'catalogue-items')
            statement.setString(5, profileName)
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic corpus/profile matched ${corpusName ?: 'catalogue-items'} / ${profileName}")
                }
                modelIndex((UUID) rs.getObject('id'))
            }
        }
    }

    Map<String, Object> deleteModelIndex(UUID mauroModelId, String profileName) {
        int deleted
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_model_index mi
                 USING semantic.embedding_profile p
                 WHERE mi.embedding_profile_id = p.id
                   AND mi.mauro_model_id = ?
                   AND p.name = ?
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setString(2, profileName)
            deleted = statement.executeUpdate()
        }
        [mauroModelId: mauroModelId.toString(), profileName: profileName, deleted: deleted] as Map<String, Object>
    }

    List<Map<String, Object>> staleEnabledModelIndexes() {
        modelIndexes().findAll {Map<String, Object> row ->
            Boolean.TRUE.equals(row.get('enabled')) && row.get('status') != 'READY'
        } as List<Map<String, Object>>
    }

    Map<String, Object> modelIndex(UUID modelIndexId) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT mi.id,
                        mi.mauro_model_id,
                        COALESCE(model_label.label, mi.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        mi.enabled,
                        mi.status,
                        mi.last_indexed_at,
                        mi.stale_requested_at,
                        mi.indexing_started_at,
                        mi.last_checked_at,
                        mi.last_error,
                        mi.created_at,
                        mi.updated_at,
                        c.name AS corpus_name,
                        p.name AS profile_name,
                        p.provider,
                        p.embedding_model,
                        p.dimension,
                        0::bigint AS chunks,
                        0::bigint AS embeddings
                 FROM semantic.semantic_model_index mi
                      JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = mi.embedding_profile_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = mi.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 WHERE mi.id = ?
             ''')) {
            statement.setObject(1, modelIndexId)
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic model index ${modelIndexId}")
                }
                modelIndexMap(rs)
            }
        }
    }

    Map<String, Object> modelIndex(UUID mauroModelId, String profileName, String corpusName = null) {
        modelIndexes().find {Map<String, Object> row ->
            row.get('mauroModelId') == mauroModelId.toString() &&
                row.get('profileName') == profileName &&
                (corpusName == null || row.get('corpusName') == corpusName)
        }
    }

    void updateModelIndexStatus(UUID mauroModelId, String profileName, String corpusName, String status, String error = null) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.semantic_model_index mi
                 SET status = ?,
                     last_indexed_at = CASE WHEN ? = 'READY' THEN now() ELSE last_indexed_at END,
                     indexing_started_at = CASE WHEN ? = 'INDEXING' THEN now() ELSE indexing_started_at END,
                     stale_requested_at = CASE WHEN ? = 'READY' THEN NULL ELSE stale_requested_at END,
                     last_error = ?,
                     updated_at = now()
                 FROM semantic.embedding_profile p,
                      semantic.semantic_corpus c
                 WHERE p.id = mi.embedding_profile_id
                   AND c.id = mi.corpus_id
                   AND mi.mauro_model_id = ?
                   AND p.name = ?
                   AND c.name = ?
             ''')) {
            statement.setString(1, status)
            statement.setString(2, status)
            statement.setString(3, status)
            statement.setString(4, status)
            statement.setString(5, error)
            statement.setObject(6, mauroModelId)
            statement.setString(7, profileName)
            statement.setString(8, corpusName ?: 'catalogue-items')
            statement.executeUpdate()
        }
    }

    void markModelIndexStale(UUID mauroModelId, String profileName, String corpusName, String reason = null) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.semantic_model_index mi
                 SET status = CASE WHEN mi.status = 'INDEXING' THEN mi.status ELSE 'STALE' END,
                     stale_requested_at = now(),
                     last_error = NULL,
                     updated_at = now()
                 FROM semantic.embedding_profile p,
                      semantic.semantic_corpus c
                 WHERE p.id = mi.embedding_profile_id
                   AND c.id = mi.corpus_id
                   AND mi.mauro_model_id = ?
                   AND p.name = ?
                   AND c.name = ?
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setString(2, profileName)
            statement.setString(3, corpusName ?: 'catalogue-items')
            statement.executeUpdate()
        }
    }

    boolean modelIndexChangedDuringRun(UUID mauroModelId, String profileName, String corpusName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT mi.stale_requested_at IS NOT NULL
                    AND mi.indexing_started_at IS NOT NULL
                    AND mi.stale_requested_at > mi.indexing_started_at
                 FROM semantic.semantic_model_index mi
                      JOIN semantic.embedding_profile p ON p.id = mi.embedding_profile_id
                      JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                 WHERE mi.mauro_model_id = ?
                   AND p.name = ?
                   AND c.name = ?
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setString(2, profileName)
            statement.setString(3, corpusName ?: 'catalogue-items')
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() && rs.getBoolean(1)
            }
        }
    }

    Map<String, Object> activeJobForModelIndex(UUID mauroModelId, String profileName, String corpusName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT job.id,
                        job.mauro_model_id,
                        COALESCE(model_label.label, job.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        job.status,
                        job.force,
                        job.max_rows,
                        job.batch_size,
                        job.result::text AS result_json,
                        job.error,
                        job.started_at,
                        job.completed_at,
                        job.created_at,
                        job.updated_at,
                        c.name AS corpus_name,
                        p.name AS profile_name
                 FROM semantic.semantic_index_job job
                      LEFT JOIN semantic.semantic_corpus c ON c.id = job.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = job.embedding_profile_id
                      LEFT JOIN semantic.semantic_model_index mi ON mi.id = job.model_index_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = job.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 WHERE job.mauro_model_id = ?
                   AND p.name = ?
                   AND c.name = ?
                   AND job.status IN ('QUEUED', 'TO_RESTART', 'RUNNING')
                 ORDER BY job.created_at ASC, job.id ASC
                 LIMIT 1
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setString(2, profileName)
            statement.setString(3, corpusName ?: 'catalogue-items')
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() ? jobMap(rs) : null
            }
        }
    }

    UUID createIndexJob(UUID mauroModelId, String profileName, String corpusName, boolean force, Integer maxRows, Integer batchSize) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_index_job (
                     model_index_id, corpus_id, mauro_model_id, embedding_profile_id, status, force, max_rows, batch_size
                 )
                 SELECT mi.id, mi.corpus_id, mi.mauro_model_id, mi.embedding_profile_id, 'QUEUED', ?, ?, ?
                 FROM semantic.semantic_model_index mi
                      JOIN semantic.embedding_profile p ON p.id = mi.embedding_profile_id
                      JOIN semantic.semantic_corpus c ON c.id = mi.corpus_id
                 WHERE mi.mauro_model_id = ?
                   AND p.name = ?
                   AND c.name = ?
                 RETURNING id
             ''')) {
            statement.setBoolean(1, force)
            setInteger(statement, 2, maxRows)
            setInteger(statement, 3, batchSize)
            statement.setObject(4, mauroModelId)
            statement.setString(5, profileName)
            statement.setString(6, corpusName ?: 'catalogue-items')
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic model index for ${mauroModelId} / ${profileName}")
                }
                (UUID) rs.getObject('id')
            }
        }
    }

    @Connectable
    Map<String, Object> job(UUID jobId) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT job.id,
                        job.mauro_model_id,
                        COALESCE(model_label.label, job.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        job.status,
                        job.force,
                        job.max_rows,
                        job.batch_size,
                        job.result::text AS result_json,
                        job.error,
                        job.started_at,
                        job.completed_at,
                        job.created_at,
                        job.updated_at,
                        c.name AS corpus_name,
                        p.name AS profile_name
                 FROM semantic.semantic_index_job job
                      LEFT JOIN semantic.semantic_corpus c ON c.id = job.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = job.embedding_profile_id
                      LEFT JOIN semantic.semantic_model_index mi ON mi.id = job.model_index_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = job.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 WHERE job.id = ?
             ''')) {
            statement.setObject(1, jobId)
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic indexing job ${jobId}")
                }
                jobMap(rs)
            }
        }
    }

    List<Map<String, Object>> jobs(List<String> statuses = null) {
        String statusClause = statuses == null || statuses.isEmpty() ? '' : 'WHERE job.status = ANY (?::varchar[])'
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT job.id,
                        job.mauro_model_id,
                        COALESCE(model_label.label, job.mauro_model_id::text) AS mauro_model_label,
                        mi.label AS model_index_label,
                        job.status,
                        job.force,
                        job.max_rows,
                        job.batch_size,
                        job.result::text AS result_json,
                        job.error,
                        job.started_at,
                        job.completed_at,
                        job.created_at,
                        job.updated_at,
                        c.name AS corpus_name,
                        p.name AS profile_name
                 FROM semantic.semantic_index_job job
                      LEFT JOIN semantic.semantic_corpus c ON c.id = job.corpus_id
                      JOIN semantic.embedding_profile p ON p.id = job.embedding_profile_id
                      LEFT JOIN semantic.semantic_model_index mi ON mi.id = job.model_index_id
                      LEFT JOIN LATERAL (
                          SELECT sd.label
                          FROM search.search_domains sd
                          WHERE sd.id = job.mauro_model_id
                          ORDER BY CASE
                              WHEN sd.domain_type IN ('Folder', 'DataModel', 'Terminology', 'CodeSet') THEN 0
                              ELSE 1
                          END
                          LIMIT 1
                      ) model_label ON TRUE
                 ${statusClause}
                 ORDER BY job.created_at DESC, job.id DESC
             """)) {
            if (statuses != null && !statuses.isEmpty()) {
                statement.setArray(1, connection.createArrayOf('varchar', statuses.toArray(new String[0])))
            }
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>()
                while (rs.next()) {
                    rows.add(jobMap(rs))
                }
                rows
            }
        }
    }

    List<Map<String, Object>> recoverableJobs() {
        jobs(['QUEUED', 'RUNNING', 'INTERRUPTED', 'TO_RESTART'] as List<String>)
    }

    void updateJobStatus(UUID jobId, String status, Map<String, Object> result = null, String error = null) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.semantic_index_job
                 SET status = ?,
                     result = COALESCE(?::jsonb, result),
                     error = ?,
                     started_at = CASE WHEN ? = 'RUNNING' AND started_at IS NULL THEN now() ELSE started_at END,
                     completed_at = CASE
                         WHEN ? IN ('RUNNING', 'TO_RESTART') THEN NULL
                         WHEN ? IN ('SUCCEEDED', 'FAILED', 'CANCELLED', 'INTERRUPTED') THEN now()
                         ELSE completed_at
                     END,
                     updated_at = now()
                 WHERE id = ?
                   AND NOT (status = 'CANCELLED' AND ? = 'RUNNING')
             ''')) {
            statement.setString(1, status)
            statement.setString(2, result == null ? null : JsonOutput.toJson(result))
            statement.setString(3, error)
            statement.setString(4, status)
            statement.setString(5, status)
            statement.setString(6, status)
            statement.setObject(7, jobId)
            statement.setString(8, status)
            int updated = statement.executeUpdate()
            if (updated > 0) {
                insertJobEvent(connection, jobId, status, result, error)
            }
        }
    }

    void appendJobEvent(UUID jobId, String status, Map<String, Object> event = null, String error = null) {
        try (Connection connection = dataSource.connection) {
            insertJobEvent(connection, jobId, status, event, error)
        }
    }

    private static void insertJobEvent(Connection connection,
                                       UUID jobId,
                                       String status,
                                       Map<String, Object> event = null,
                                       String error = null) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>()
        if (event != null) {
            payload.putAll(event)
        }
        if (error != null) {
            payload.put('error', error)
        }
        payload.put('status', status)
        try (PreparedStatement statement = connection.prepareStatement('''
                 INSERT INTO semantic.semantic_index_job_event (job_id, status, event)
                 VALUES (?, ?, ?::jsonb)
             ''')) {
            statement.setObject(1, jobId)
            statement.setString(2, status)
            statement.setString(3, JsonOutput.toJson(payload))
            statement.executeUpdate()
        }
    }

    @Connectable
    String jobEvents(UUID jobId) {
        List<String> lines = jobEventLinesAfter(jobId, 0L)
        lines.add(jobSnapshotLine(jobId))
        lines.join('\n') + (lines.isEmpty() ? '' : '\n')
    }

    @Connectable
    List<String> jobEventLinesAfter(UUID jobId, long afterSequence) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT id, status, event::text AS event_json, created_at
                 FROM semantic.semantic_index_job_event
                 WHERE job_id = ?
                   AND id > ?
                 ORDER BY id
             ''')) {
            statement.setObject(1, jobId)
            statement.setLong(2, Math.max(afterSequence, 0L))
            try (ResultSet rs = statement.executeQuery()) {
                List<String> lines = new ArrayList<String>()
                while (rs.next()) {
                    Map<String, Object> line = new LinkedHashMap<String, Object>()
                    line.put('sequence', rs.getLong('id'))
                    line.put('jobId', jobId.toString())
                    line.put('status', rs.getString('status'))
                    line.put('createdAt', rs.getTimestamp('created_at')?.toInstant()?.toString())
                    line.put('event', parseJson(rs.getString('event_json')))
                    lines.add(JsonOutput.toJson(line))
                }
                lines
            }
        }
    }

    @Connectable
    String jobSnapshotLine(UUID jobId) {
        Map<String, Object> currentJob = job(jobId)
        JsonOutput.toJson([
            snapshot: true,
            jobId: String.valueOf(currentJob.get('jobId')),
            status: String.valueOf(currentJob.get('status')),
            observedAt: Instant.now().toString(),
            updatedAt: currentJob.get('updatedAt'),
            event: currentJob.get('result') ?: [:]
        ] as Map<String, Object>)
    }

    boolean jobCancelled(UUID jobId) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('SELECT status = ? FROM semantic.semantic_index_job WHERE id = ?')) {
            statement.setString(1, 'CANCELLED')
            statement.setObject(2, jobId)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next() && rs.getBoolean(1)
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
             PreparedStatement allowedStatement = connection.prepareStatement('''
                 SELECT chunk_delete_api_enabled
                 FROM semantic.semantic_corpus
                 WHERE name = ?
             ''');
             PreparedStatement statement = connection.prepareStatement('''
                 DELETE FROM semantic.semantic_chunk chunk
                 USING semantic.semantic_corpus corpus
                 WHERE chunk.corpus_id = corpus.id
                   AND corpus.name = ?
             ''')) {
            allowedStatement.setString(1, corpusName)
            try (ResultSet rs = allowedStatement.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalArgumentException("No semantic corpus named ${corpusName}")
                }
                if (!rs.getBoolean('chunk_delete_api_enabled')) {
                    throw new IllegalArgumentException("Semantic corpus ${corpusName} does not allow API chunk deletion")
                }
            }
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
                ${catalogueGeneratedChunksSql(false)}
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

    boolean modelIndexNeedsRefresh(String corpusName, UUID mauroModelId, String profileName) {
        String sql = """
            WITH source_rows AS (
                ${catalogueSourceRowsSql(Collections.<String>emptyList(), mauroModelId, null)}
            ),
            chunks AS (
                ${catalogueGeneratedChunksSql(true)}
            ),
            scoped_model_ids(id) AS (
                ${scopedModelIdsSql()}
            )
            SELECT
                EXISTS (
                    SELECT 1
                    FROM chunks generated
                         JOIN semantic.semantic_corpus corpus ON corpus.name = ?
                         LEFT JOIN semantic.semantic_chunk existing
                              ON existing.corpus_id = corpus.id
                             AND existing.source_type = 'catalogue-item'
                             AND existing.source_id = generated.id
                             AND existing.source_domain_type = generated.domain_type
                             AND existing.chunk_kind = generated.chunk_kind
                             AND existing.chunk_ordinal = generated.chunk_ordinal
                    WHERE existing.id IS NULL
                       OR existing.source_label IS DISTINCT FROM generated.label
                       OR existing.mauro_model_id IS DISTINCT FROM generated.model_id
                       OR existing.chunk_group IS DISTINCT FROM generated.chunk_group
                       OR existing.source_text IS DISTINCT FROM generated.source_text
                       OR existing.content_hash IS DISTINCT FROM encode(sha256(convert_to(generated.source_text, 'UTF8')), 'hex')
                       OR existing.date_created IS DISTINCT FROM generated.date_created
                       OR existing.last_updated IS DISTINCT FROM generated.last_updated
                )
                OR EXISTS (
                    SELECT 1
                    FROM semantic.semantic_chunk existing
                         JOIN semantic.semantic_corpus corpus ON corpus.id = existing.corpus_id
                         LEFT JOIN chunks generated
                              ON generated.id = existing.source_id
                             AND generated.domain_type = existing.source_domain_type
                             AND generated.chunk_kind = existing.chunk_kind
                             AND generated.chunk_ordinal = existing.chunk_ordinal
                    WHERE corpus.name = ?
                      AND existing.source_type = 'catalogue-item'
                      AND existing.mauro_model_id IN (SELECT id FROM scoped_model_ids)
                      AND generated.id IS NULL
                )
                OR EXISTS (
                    SELECT 1
                    FROM chunks generated
                         JOIN semantic.semantic_corpus corpus ON corpus.name = ?
                         JOIN semantic.semantic_chunk existing
                              ON existing.corpus_id = corpus.id
                             AND existing.source_type = 'catalogue-item'
                             AND existing.source_id = generated.id
                             AND existing.source_domain_type = generated.domain_type
                             AND existing.chunk_kind = generated.chunk_kind
                             AND existing.chunk_ordinal = generated.chunk_ordinal
                    WHERE NOT EXISTS (
                        SELECT 1
                        FROM semantic.semantic_embedding embedding
                             JOIN semantic.embedding_profile profile ON profile.id = embedding.embedding_profile_id
                        WHERE embedding.chunk_id = existing.id
                          AND embedding.content_hash = existing.content_hash
                          AND profile.name = ?
                    )
                ) AS needs_refresh
        """
        boolean needsRefresh
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, Collections.<String>emptyList(), mauroModelId, null)
            statement.setObject(index++, mauroModelId)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            statement.setString(index++, corpusName ?: 'catalogue-items')
            statement.setString(index++, corpusName ?: 'catalogue-items')
            statement.setString(index, profileName)
            try (ResultSet rs = statement.executeQuery()) {
                rs.next()
                needsRefresh = rs.getBoolean('needs_refresh')
            }
        }
        updateModelIndexCheckedAt(mauroModelId, profileName)
        needsRefresh
    }

    private void updateModelIndexCheckedAt(UUID mauroModelId, String profileName) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 UPDATE semantic.semantic_model_index mi
                 SET last_checked_at = now(),
                     updated_at = now()
                 FROM semantic.embedding_profile p
                 WHERE p.id = mi.embedding_profile_id
                   AND mi.mauro_model_id = ?
                   AND p.name = ?
             ''')) {
            statement.setObject(1, mauroModelId)
            statement.setString(2, profileName)
            statement.executeUpdate()
        }
    }

    int reconcileCatalogueChunks(String corpusName, List<String> domainTypes = [], UUID mauroModelId = null, Integer maxRows = null) {
        Map<String, Integer> result = reconcileCatalogueChunksDetailed(corpusName, domainTypes, mauroModelId, maxRows)
        (result.get('upsertedChunks') ?: 0) + (result.get('deletedChunks') ?: 0)
    }

    Map<String, Integer> reconcileCatalogueChunksDetailed(String corpusName, List<String> domainTypes = [], UUID mauroModelId = null, Integer maxRows = null) {
        UUID corpusId = corpusId(corpusName ?: 'catalogue-items')
        if (corpusId == null) {
            return [upsertedChunks: 0, deletedChunks: 0, changedChunks: 0] as Map<String, Integer>
        }
        String sql = """
            WITH source_rows AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows)}
            ),
            chunks AS (
                ${catalogueGeneratedChunksSql(true)}
            )
            INSERT INTO semantic.semantic_chunk (
                corpus_id, source_type, source_id, source_domain_type, source_label, mauro_model_id,
                chunk_kind, chunk_group, chunk_ordinal, source_text, content_hash, date_created, last_updated
            )
            SELECT ?::uuid,
                   'catalogue-item',
                   id,
                   domain_type,
                   label,
                   model_id,
                   chunk_kind,
                   chunk_group,
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
                          chunk_group = EXCLUDED.chunk_group,
                          source_text = EXCLUDED.source_text,
                          content_hash = EXCLUDED.content_hash,
                          date_created = EXCLUDED.date_created,
                          last_updated = EXCLUDED.last_updated,
                          indexed_at = now()
            WHERE semantic.semantic_chunk.source_domain_type IS DISTINCT FROM EXCLUDED.source_domain_type
               OR semantic.semantic_chunk.source_label IS DISTINCT FROM EXCLUDED.source_label
               OR semantic.semantic_chunk.mauro_model_id IS DISTINCT FROM EXCLUDED.mauro_model_id
               OR semantic.semantic_chunk.chunk_group IS DISTINCT FROM EXCLUDED.chunk_group
               OR semantic.semantic_chunk.source_text IS DISTINCT FROM EXCLUDED.source_text
               OR semantic.semantic_chunk.content_hash IS DISTINCT FROM EXCLUDED.content_hash
               OR semantic.semantic_chunk.date_created IS DISTINCT FROM EXCLUDED.date_created
               OR semantic.semantic_chunk.last_updated IS DISTINCT FROM EXCLUDED.last_updated
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setObject(index, corpusId)
            int upserted = statement.executeUpdate()
            int deleted = deleteStaleCatalogueChunks(connection, corpusId, domainTypes, mauroModelId, maxRows)
            [
                upsertedChunks: upserted,
                deletedChunks: deleted,
                changedChunks: upserted + deleted
            ] as Map<String, Integer>
        }
    }

    private int deleteStaleCatalogueChunks(Connection connection,
                                           UUID corpusId,
                                           List<String> domainTypes = [],
                                           UUID mauroModelId = null,
                                           Integer maxRows = null) {
        String scopedModelIdsCte = mauroModelId == null ? '' : """,
            scoped_model_ids(id) AS (
                ${scopedModelIdsSql()}
            )
        """
        String scopedModelClause = mauroModelId == null ? '' : 'AND existing.mauro_model_id IN (SELECT id FROM scoped_model_ids)'
        String sql = """
            WITH source_rows AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows)}
            ),
            chunks AS (
                ${catalogueGeneratedChunksSql(false)}
            )
            ${scopedModelIdsCte}
            DELETE FROM semantic.semantic_chunk existing
            WHERE existing.corpus_id = ?::uuid
              AND existing.source_type = 'catalogue-item'
              ${scopedModelClause}
              AND NOT EXISTS (
                  SELECT 1
                  FROM chunks generated
                  WHERE generated.id = existing.source_id
                    AND generated.domain_type = existing.source_domain_type
                    AND generated.chunk_kind = existing.chunk_kind
                    AND generated.chunk_ordinal = existing.chunk_ordinal
              )
        """
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            if (mauroModelId != null) {
                statement.setObject(index++, mauroModelId)
            }
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
                   c.chunk_group,
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
        String mauroModelClause = modelScopeClause(mauroModelId, 'sd.model_id')
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
                     chunk_kind, chunk_group, chunk_ordinal, source_text, content_hash, date_created, last_updated
                 )
                 VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                 ON CONFLICT (corpus_id, source_type, source_id, chunk_kind, chunk_ordinal)
                 DO UPDATE SET source_domain_type = EXCLUDED.source_domain_type,
                               source_label = EXCLUDED.source_label,
                               mauro_model_id = EXCLUDED.mauro_model_id,
                               chunk_group = EXCLUDED.chunk_group,
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
            statement.setString(8, chunk.chunkGroup ?: chunkGroupForKind(chunk.chunkKind))
            statement.setInt(9, chunk.chunkOrdinal ?: 0)
            statement.setString(10, chunk.sourceText)
            statement.setString(11, chunk.contentHash)
            setInstant(statement, 12, chunk.dateCreated)
            setInstant(statement, 13, chunk.lastUpdated)
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

    List<Map<String, Object>> staleEmbeddingCountsByChunkKind(EmbeddingProfile profile, int limit = 20) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('''
                 SELECT c.chunk_group,
                        c.chunk_kind,
                        c.source_domain_type,
                        count(*) AS stale_count
                 FROM semantic.semantic_embedding e
                      JOIN semantic.semantic_chunk c ON c.id = e.chunk_id
                 WHERE e.embedding_profile_id = ?
                   AND e.content_hash <> c.content_hash
                 GROUP BY c.chunk_group, c.chunk_kind, c.source_domain_type
                 ORDER BY stale_count DESC
                 LIMIT ?
             ''')) {
            statement.setObject(1, profile.id)
            statement.setInt(2, Math.max(limit, 1))
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = []
                while (rs.next()) {
                    rows.add([
                        chunkGroup: rs.getString('chunk_group'),
                        chunkKind: rs.getString('chunk_kind'),
                        sourceDomainType: rs.getString('source_domain_type'),
                        staleCount: Long.valueOf(rs.getLong('stale_count'))
                    ] as Map<String, Object>)
                }
                rows
            }
        }
    }

    int reuseEmbeddingsForMatchingContentHashes(EmbeddingProfile profile,
                                                String corpusName,
                                                List<String> domainTypes = [],
                                                UUID mauroModelId = null,
                                                Integer maxRows = null) {
        String filterClause = chunkSourceFilterClause(domainTypes, mauroModelId, maxRows)
        String sql = """
            WITH selected_sources AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows, true)}
            ),
            reusable AS (
                SELECT c.id AS chunk_id,
                       c.content_hash,
                       existing.embedding,
                       c.chunk_group
                FROM semantic.semantic_chunk c
                     JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                     JOIN LATERAL (
                         SELECT reusable_embedding.embedding
                         FROM semantic.semantic_embedding reusable_embedding
                         WHERE reusable_embedding.embedding_profile_id = ?
                           AND reusable_embedding.content_hash = c.content_hash
                         ORDER BY reusable_embedding.updated_at DESC
                         LIMIT 1
                     ) existing ON TRUE
                     ${filterClause}
                WHERE corpus.name = ?
                  AND NOT EXISTS (
                      SELECT 1
                      FROM semantic.semantic_embedding current_embedding
                      WHERE current_embedding.chunk_id = c.id
                        AND current_embedding.embedding_profile_id = ?
                        AND current_embedding.content_hash = c.content_hash
                  )
            )
            INSERT INTO semantic.semantic_embedding (chunk_id, embedding_profile_id, content_hash, embedding, chunk_group)
            SELECT reusable.chunk_id,
                   ?,
                   reusable.content_hash,
                   reusable.embedding,
                   reusable.chunk_group
            FROM reusable
            ON CONFLICT (chunk_id, embedding_profile_id)
            DO UPDATE SET content_hash = EXCLUDED.content_hash,
                          embedding = EXCLUDED.embedding,
                          chunk_group = EXCLUDED.chunk_group,
                          updated_at = now()
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setObject(index++, profile.id)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            statement.setObject(index++, profile.id)
            statement.setObject(index, profile.id)
            statement.executeUpdate()
        }
    }

    int syncEmbeddingChunkGroups(String corpusName,
                                 List<String> domainTypes = [],
                                 UUID mauroModelId = null,
                                 Integer maxRows = null) {
        String filterClause = chunkSourceFilterClause(domainTypes, mauroModelId, maxRows)
        String sql = """
            WITH selected_sources AS (
                ${catalogueSourceRowsSql(domainTypes, mauroModelId, maxRows, true)}
            )
            UPDATE semantic.semantic_embedding embedding
            SET chunk_group = c.chunk_group,
                updated_at = now()
            FROM semantic.semantic_chunk c
                 JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                 ${filterClause}
            WHERE embedding.chunk_id = c.id
              AND corpus.name = ?
              AND embedding.chunk_group IS DISTINCT FROM c.chunk_group
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = bindCatalogueSourceRows(statement, connection, domainTypes, mauroModelId, maxRows)
            statement.setString(index, corpusName ?: 'catalogue-items')
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
                 INSERT INTO semantic.semantic_embedding (chunk_id, embedding_profile_id, content_hash, embedding, chunk_group)
                 VALUES (?, ?, ?, ?::vector, ?)
                 ON CONFLICT (chunk_id, embedding_profile_id)
                 DO UPDATE SET content_hash = EXCLUDED.content_hash,
                               embedding = EXCLUDED.embedding,
                               chunk_group = EXCLUDED.chunk_group,
                               updated_at = now()
             ''')) {
            for (int i = 0; i < chunks.size(); i++) {
                SemanticChunk chunk = chunks.get(i)
                statement.setObject(1, chunk.id)
                statement.setObject(2, profile.id)
                statement.setString(3, chunk.contentHash)
                statement.setString(4, vectorLiteral(embeddings.get(i)))
                statement.setString(5, chunk.chunkGroup ?: chunkGroupForKind(chunk.chunkKind))
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    void dropVectorIndex(EmbeddingProfile profile) {
        executeIndexStatement("DROP INDEX IF EXISTS semantic.${vectorIndexName(profile)}")
        executeIndexStatement("DROP INDEX IF EXISTS semantic.${vectorIndexName(profile, 'catalogue')}")
        executeIndexStatement("DROP INDEX IF EXISTS semantic.${vectorIndexName(profile, 'context')}")
    }

    void createVectorIndex(EmbeddingProfile profile) {
        String vectorCast = "vector(${Math.max(profile.dimension ?: 0, 1)})"
        String operatorClass = vectorOperatorClass(profile)
        String profileId = profile.id.toString().replace("'", "''")
        for (String chunkGroup : ['catalogue', 'context']) {
            String safeChunkGroup = chunkGroup.replace("'", "''")
            String indexName = vectorIndexName(profile, chunkGroup)
            executeIndexStatementWithProgress(indexName, """
                CREATE INDEX IF NOT EXISTS ${vectorIndexName(profile, chunkGroup)}
                ON semantic.semantic_embedding
                USING hnsw ((embedding::${vectorCast}) ${operatorClass})
                WHERE embedding_profile_id = '${profileId}'
                  AND chunk_group = '${safeChunkGroup}'
            """)
        }
    }

    boolean vectorIndexExists(EmbeddingProfile profile) {
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement('SELECT to_regclass(?) IS NOT NULL')) {
            for (String chunkGroup : ['catalogue', 'context']) {
                statement.setString(1, "semantic.${vectorIndexName(profile, chunkGroup)}")
                try (ResultSet rs = statement.executeQuery()) {
                    rs.next()
                    if (!rs.getBoolean(1)) {
                        return false
                    }
                }
            }
            true
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

    boolean refreshAdministeredItemContextIfExists() {
        try (Connection connection = dataSource.connection;
             PreparedStatement existsStatement = connection.prepareStatement("SELECT to_regclass('search.administered_item_context') IS NOT NULL")) {
            try (ResultSet rs = existsStatement.executeQuery()) {
                rs.next()
                if (!rs.getBoolean(1)) {
                    return false
                }
            }
            try (Statement refreshStatement = connection.createStatement()) {
                refreshStatement.execute('REFRESH MATERIALIZED VIEW CONCURRENTLY search.administered_item_context')
                true
            }
        }
    }

    List<SemanticCandidate> search(EmbeddingProfile profile,
                                   float[] queryEmbedding,
                                   String corpusName,
                                   List<String> domainTypes,
                                   UUID mauroModelId,
                                   int topN,
                                   String chunkGroup = 'catalogue') {
        if (mauroModelId != null) {
            return searchWithinModelScope(profile, queryEmbedding, corpusName, domainTypes, mauroModelId, topN, chunkGroup)
        }
        boolean hasDomainTypes = domainTypes != null && !domainTypes.isEmpty()
        String domainClause = hasDomainTypes ? ' AND target.target_id IS NOT NULL' : ''
        String mauroModelClause = modelScopeClause(mauroModelId, 'c.mauro_model_id')
        String chunkGroupClause = chunkGroup == null || chunkGroup.trim().isEmpty() ? '' : ' AND e.chunk_group = ?'
        String vectorCast = "vector(${Math.max(profile.dimension ?: 0, 1)})"
        String distanceExpression = "(e.embedding::${vectorCast} <=> ?::${vectorCast})"
        String sql = """
            WITH RECURSIVE requested_domain_types AS (
                SELECT unnest(?::varchar[]) AS domain_type
            ),
            nearest_chunks AS (
                SELECT c.id AS chunk_id,
                       c.source_id,
                       c.source_domain_type,
                       c.source_label,
                       c.mauro_model_id,
                       c.chunk_kind,
                       c.chunk_group,
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
                WHERE p.id = ?
                  AND corpus.name = ?
                  AND c.chunk_kind <> 'classification'
                  AND c.source_domain_type NOT IN ('ClassificationScheme', 'Classifier')
                  ${mauroModelClause}
                  ${chunkGroupClause}
                ORDER BY e.embedding::${vectorCast} <=> ?::${vectorCast},
                         c.source_domain_type,
                         c.source_label,
                         c.source_id,
                         c.chunk_kind,
                         c.chunk_ordinal,
                         c.id
                LIMIT ?
            )
            SELECT c.chunk_id,
                   c.source_id,
                   c.source_domain_type,
                   c.source_label,
                   sd.description,
                   COALESCE(target.target_id, c.source_id) AS target_id,
                   COALESCE(target.target_domain_type, c.source_domain_type) AS target_domain_type,
                   COALESCE(target.target_label, c.source_label) AS target_label,
                   COALESCE(target.target_description, sd.description) AS target_description,
                   COALESCE(target.relation_distance, 0) AS relation_distance,
                   c.chunk_kind,
                   c.chunk_group,
                   c.chunk_ordinal,
                   c.source_text,
                   c.embedding_profile,
                   c.distance,
                   c.date_created,
                   c.last_updated
            FROM nearest_chunks c
                 LEFT JOIN search.search_domains sd ON sd.id = c.source_id AND sd.domain_type = c.source_domain_type
                 LEFT JOIN LATERAL (
                    ${projectionTargetsSql()}
                 ) target ON true
            WHERE 1 = 1
              ${domainClause}
            ORDER BY c.distance,
                     target.target_domain_type,
                     target.target_label,
                     target.target_id,
                     c.source_domain_type,
                     c.source_label,
                     c.source_id,
                     c.chunk_kind,
                     c.chunk_ordinal,
                     c.chunk_id
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            applyHnswSearchSetting(connection, chunkGroup, topN)
            int index = 1
            String queryVector = vectorLiteral(queryEmbedding)
            String[] requestedDomainTypes = hasDomainTypes ? domainTypes.toArray(new String[0]) as String[] : ['__none__'] as String[]
            statement.setArray(index++, connection.createArrayOf('varchar', requestedDomainTypes))
            statement.setString(index++, queryVector)
            statement.setObject(index++, profile.id)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (mauroModelId != null) {
                statement.setObject(index++, mauroModelId)
            }
            if (chunkGroup != null && !chunkGroup.trim().isEmpty()) {
                statement.setString(index++, chunkGroup)
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

    private List<SemanticCandidate> searchWithinModelScope(EmbeddingProfile profile,
                                                           float[] queryEmbedding,
                                                           String corpusName,
                                                           List<String> domainTypes,
                                                           UUID mauroModelId,
                                                           int topN,
                                                           String chunkGroup = 'catalogue') {
        boolean hasDomainTypes = domainTypes != null && !domainTypes.isEmpty()
        String domainClause = hasDomainTypes ? ' AND target.target_id IS NOT NULL' : ''
        String chunkGroupClause = chunkGroup == null || chunkGroup.trim().isEmpty() ? '' : ' AND e.chunk_group = ?'
        String vectorCast = "vector(${Math.max(profile.dimension ?: 0, 1)})"
        String distanceExpression = "(e.embedding::${vectorCast} <=> ?::${vectorCast})"
        String sql = """
            WITH RECURSIVE requested_domain_types AS (
                SELECT unnest(?::varchar[]) AS domain_type
            ),
            requested_scope(id) AS (
                SELECT ?::uuid
            ),
            scoped_folders(id) AS (
                SELECT folder.id
                FROM core.folder folder
                     JOIN requested_scope scope ON scope.id = folder.id
                UNION ALL
                SELECT child.id
                FROM core.folder child
                     JOIN scoped_folders parent ON child.parent_folder_id = parent.id
            ),
            scoped_model_ids(id) AS MATERIALIZED (
                SELECT scope.id
                FROM requested_scope scope
                WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
                   OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
                   OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
                UNION
                SELECT data_model.id
                FROM datamodel.data_model data_model
                     JOIN scoped_folders folder ON folder.id = data_model.folder_id
                UNION
                SELECT terminology.id
                FROM terminology.terminology terminology
                     JOIN scoped_folders folder ON folder.id = terminology.folder_id
                UNION
                SELECT code_set.id
                FROM terminology.code_set code_set
                     JOIN scoped_folders folder ON folder.id = code_set.folder_id
            ),
            nearest_chunks AS (
                SELECT c.id AS chunk_id,
                       c.source_id,
                       c.source_domain_type,
                       c.source_label,
                       c.mauro_model_id,
                       c.chunk_kind,
                       c.chunk_group,
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
                     JOIN scoped_model_ids scoped_model ON scoped_model.id = c.mauro_model_id
                WHERE p.id = ?
                  AND corpus.name = ?
                  AND c.chunk_kind <> 'classification'
                  AND c.source_domain_type NOT IN ('ClassificationScheme', 'Classifier')
                  ${chunkGroupClause}
                ORDER BY e.embedding::${vectorCast} <=> ?::${vectorCast},
                         c.source_domain_type,
                         c.source_label,
                         c.source_id,
                         c.chunk_kind,
                         c.chunk_ordinal,
                         c.id
                LIMIT ?
            )
            SELECT c.chunk_id,
                   c.source_id,
                   c.source_domain_type,
                   c.source_label,
                   sd.description,
                   COALESCE(target.target_id, c.source_id) AS target_id,
                   COALESCE(target.target_domain_type, c.source_domain_type) AS target_domain_type,
                   COALESCE(target.target_label, c.source_label) AS target_label,
                   COALESCE(target.target_description, sd.description) AS target_description,
                   COALESCE(target.relation_distance, 0) AS relation_distance,
                   c.chunk_kind,
                   c.chunk_group,
                   c.chunk_ordinal,
                   c.source_text,
                   c.embedding_profile,
                   c.distance,
                   c.date_created,
                   c.last_updated
            FROM nearest_chunks c
                 LEFT JOIN search.search_domains sd ON sd.id = c.source_id AND sd.domain_type = c.source_domain_type
                 LEFT JOIN LATERAL (
                    ${projectionTargetsSql()}
                 ) target ON true
            WHERE 1 = 1
              ${domainClause}
            ORDER BY c.distance,
                     target.target_domain_type,
                     target.target_label,
                     target.target_id,
                     c.source_domain_type,
                     c.source_label,
                     c.source_id,
                     c.chunk_kind,
                     c.chunk_ordinal,
                     c.chunk_id
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            applyHnswSearchSetting(connection, chunkGroup, topN)
            int index = 1
            String[] requestedDomainTypes = hasDomainTypes ? domainTypes.toArray(new String[0]) as String[] : ['__none__'] as String[]
            statement.setArray(index++, connection.createArrayOf('varchar', requestedDomainTypes))
            String queryVector = vectorLiteral(queryEmbedding)
            statement.setObject(index++, mauroModelId)
            statement.setString(index++, queryVector)
            statement.setObject(index++, profile.id)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (chunkGroup != null && !chunkGroup.trim().isEmpty()) {
                statement.setString(index++, chunkGroup)
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

    List<SemanticCandidate> lexicalSearch(String queryText,
                                          String corpusName,
                                          List<String> domainTypes,
                                          UUID mauroModelId,
                                          int topN,
                                          String chunkGroup = 'catalogue') {
        if (queryText == null || queryText.trim().isEmpty()) {
            return Collections.<SemanticCandidate>emptyList()
        }
        boolean hasDomainTypes = domainTypes != null && !domainTypes.isEmpty()
        String domainClause = hasDomainTypes ? ' AND target.target_id IS NOT NULL' : ''
        String mauroModelClause = modelScopeClause(mauroModelId, 'c.mauro_model_id')
        String chunkGroupClause = chunkGroup == null || chunkGroup.trim().isEmpty() ? '' : ' AND c.chunk_group = ?'
        String sql = """
            WITH requested_domain_types AS (
                SELECT unnest(?::varchar[]) AS domain_type
            ),
            query AS (
                SELECT websearch_to_tsquery('english', ?) AS tsquery
            ),
            lexical_matches AS (
                SELECT c.*,
                       ts_rank_cd(to_tsvector('english', c.source_text), query.tsquery)::float8 AS lexical_rank,
                       CASE c.chunk_kind
                           WHEN 'label' THEN 1.35
                           WHEN 'label-identifier' THEN 1.30
                           WHEN 'label-phrase' THEN 1.25
                           WHEN 'summary' THEN 1.10
                           WHEN 'term-definition' THEN 1.05
                           WHEN 'enumeration-key' THEN 1.05
                           WHEN 'enumeration-value' THEN 1.05
                           WHEN 'description-section' THEN 1.00
                           WHEN 'description' THEN 1.00
                           WHEN 'enumeration-category' THEN 0.95
                           WHEN 'classification' THEN 0.70
                           WHEN 'annotation' THEN 0.75
                           ELSE CASE WHEN c.chunk_kind LIKE 'semantic-link-%' THEN 0.80 ELSE 0.85 END
                       END AS lexical_weight
                FROM semantic.semantic_chunk c
                     JOIN semantic.semantic_corpus corpus ON corpus.id = c.corpus_id
                     CROSS JOIN query
                WHERE corpus.name = ?
                  AND query.tsquery @@ to_tsvector('english', c.source_text)
                  AND c.chunk_kind <> 'classification'
                  AND c.source_domain_type NOT IN ('ClassificationScheme', 'Classifier')
                  ${mauroModelClause}
                  ${chunkGroupClause}
            )
            SELECT c.id AS chunk_id,
                   c.source_id,
                   c.source_domain_type,
                   c.source_label,
                   sd.description,
                   COALESCE(target.target_id, c.source_id) AS target_id,
                   COALESCE(target.target_domain_type, c.source_domain_type) AS target_domain_type,
                   COALESCE(target.target_label, c.source_label) AS target_label,
                   COALESCE(target.target_description, sd.description) AS target_description,
                   COALESCE(target.relation_distance, 0) AS relation_distance,
                   c.chunk_kind,
                   c.chunk_group,
                   c.chunk_ordinal,
                   c.source_text,
                   'lexical'::varchar AS embedding_profile,
                   1.0 - LEAST(0.70, 0.55 + (c.lexical_rank * c.lexical_weight)) AS distance,
                   c.date_created,
                   c.last_updated
            FROM lexical_matches c
                 LEFT JOIN search.search_domains sd ON sd.id = c.source_id AND sd.domain_type = c.source_domain_type
                 LEFT JOIN LATERAL (
                    ${projectionTargetsSql()}
                 ) target ON true
            WHERE 1 = 1
              ${domainClause}
            ORDER BY (c.lexical_rank * c.lexical_weight) DESC,
                     c.chunk_group,
                     c.chunk_kind,
                     c.source_domain_type,
                     c.source_label,
                     c.source_id,
                     c.chunk_ordinal,
                     c.id
            LIMIT ?
        """
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1
            String[] requestedDomainTypes = hasDomainTypes ? domainTypes.toArray(new String[0]) as String[] : ['__none__'] as String[]
            statement.setArray(index++, connection.createArrayOf('varchar', requestedDomainTypes))
            statement.setString(index++, queryText)
            statement.setString(index++, corpusName ?: 'catalogue-items')
            if (mauroModelId != null) {
                statement.setObject(index++, mauroModelId)
            }
            if (chunkGroup != null && !chunkGroup.trim().isEmpty()) {
                statement.setString(index++, chunkGroup)
            }
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

    List<SearchResultsDTO> projectSearchResults(List<SearchResultsDTO> sourceItems,
                                                List<String> targetDomainTypes) {
        if (sourceItems == null || sourceItems.isEmpty() || targetDomainTypes == null || targetDomainTypes.isEmpty()) {
            return sourceItems ?: [] as List<SearchResultsDTO>
        }
        List<SearchResultsDTO> projected = new ArrayList<SearchResultsDTO>()
        try (Connection connection = dataSource.connection;
             PreparedStatement statement = connection.prepareStatement("""
                 WITH requested_domain_types AS (
                     SELECT unnest(?::varchar[]) AS domain_type
                 ),
                 source_item AS (
                     SELECT ?::uuid AS source_id,
                            ?::varchar AS source_domain_type,
                            ?::varchar AS source_label,
                            ?::uuid AS model_id,
                            ?::text AS source_description
                 )
                 SELECT DISTINCT
                        COALESCE(target.target_id, source_item.source_id) AS target_id,
                        COALESCE(target.target_domain_type, source_item.source_domain_type) AS target_domain_type,
                        COALESCE(target.target_label, source_item.source_label) AS target_label,
                        COALESCE(target.target_description, source_item.source_description) AS target_description,
                        COALESCE(target.relation_distance, 0) AS relation_distance
                 FROM source_item
                      LEFT JOIN search.search_domains sd
                        ON sd.id = source_item.source_id
                       AND sd.domain_type = source_item.source_domain_type
                      LEFT JOIN LATERAL (
                         SELECT source_item.source_id AS source_id,
                                source_item.source_domain_type AS source_domain_type,
                                source_item.source_label AS source_label,
                                source_item.model_id AS mauro_model_id
                      ) c ON true
                      LEFT JOIN LATERAL (
                         ${projectionTargetsSql()}
                      ) target ON true
                 WHERE target.target_id IS NOT NULL
                 ORDER BY relation_distance, target_label
             """)) {
            for (SearchResultsDTO item : sourceItems) {
                int index = 1
                statement.setArray(index++, connection.createArrayOf('varchar', targetDomainTypes.toArray(new String[0])))
                statement.setObject(index++, item.id)
                statement.setString(index++, item.domainType)
                statement.setString(index++, item.label)
                statement.setObject(index++, item.modelId)
                statement.setString(index++, item.description)
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        SearchResultsDTO dto = new SearchResultsDTO(
                            id: (UUID) rs.getObject('target_id'),
                            domainType: rs.getString('target_domain_type'),
                            label: rs.getString('target_label'),
                            description: rs.getString('target_description'),
                            tsRank: item.tsRank
                        )
                        projected.add(dto)
                    }
                }
            }
        }
        projected
    }

    private void applyHnswSearchSetting(Connection connection, String chunkGroup, int topN) {
        int configuredEfSearch = chunkGroup == 'context' ? hnswContextEfSearch : hnswCatalogueEfSearch
        int efSearch = Math.max(configuredEfSearch, Math.min(Math.max(topN, 1), 1000))
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET hnsw.ef_search = ${efSearch}")
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

    private static Map<String, Object> corpusMap(ResultSet rs) {
        [
            id: String.valueOf(rs.getObject('id')),
            name: rs.getString('name'),
            source: rs.getString('source'),
            description: rs.getString('description'),
            enabled: rs.getBoolean('enabled'),
            origin: rs.getString('origin'),
            apiVisible: rs.getBoolean('api_visible'),
            apiManageable: rs.getBoolean('api_manageable'),
            chunkDeleteApiEnabled: rs.getBoolean('chunk_delete_api_enabled'),
            createdAt: rs.getTimestamp('created_at')?.toInstant()?.toString(),
            updatedAt: rs.getTimestamp('updated_at')?.toInstant()?.toString()
        ] as Map<String, Object>
    }

    private static Map<String, Object> modelIndexMap(ResultSet rs) {
        [
            id: String.valueOf(rs.getObject('id')),
            mauroModelId: String.valueOf(rs.getObject('mauro_model_id')),
            mauroModelLabel: rs.getString('mauro_model_label'),
            label: rs.getString('model_index_label'),
            corpusName: rs.getString('corpus_name'),
            profileName: rs.getString('profile_name'),
            provider: rs.getString('provider'),
            embeddingModel: rs.getString('embedding_model'),
            dimension: rs.getInt('dimension'),
            enabled: rs.getBoolean('enabled'),
            status: rs.getString('status'),
            lastIndexedAt: rs.getTimestamp('last_indexed_at')?.toInstant()?.toString(),
            staleRequestedAt: timestampString(rs, 'stale_requested_at'),
            indexingStartedAt: timestampString(rs, 'indexing_started_at'),
            lastCheckedAt: timestampString(rs, 'last_checked_at'),
            lastError: rs.getString('last_error'),
            chunks: rs.getLong('chunks'),
            embeddings: rs.getLong('embeddings'),
            createdAt: rs.getTimestamp('created_at')?.toInstant()?.toString(),
            updatedAt: rs.getTimestamp('updated_at')?.toInstant()?.toString()
        ] as Map<String, Object>
    }

    private static Map<String, Object> modelIndexSummaryMap(ResultSet rs) {
        [
            id: String.valueOf(rs.getObject('id')),
            mauroModelId: String.valueOf(rs.getObject('mauro_model_id')),
            mauroModelLabel: rs.getString('mauro_model_label'),
            label: rs.getString('model_index_label'),
            corpusName: rs.getString('corpus_name'),
            profileName: rs.getString('profile_name'),
            enabled: rs.getBoolean('enabled'),
            status: rs.getString('status'),
            lastError: rs.getString('last_error')
        ] as Map<String, Object>
    }

    private static String timestampString(ResultSet rs, String column) {
        try {
            rs.getTimestamp(column)?.toInstant()?.toString()
        } catch (SQLException ignored) {
            null
        }
    }

    private static Map<String, Object> jobMap(ResultSet rs) {
        [
            jobId: String.valueOf(rs.getObject('id')),
            mauroModelId: String.valueOf(rs.getObject('mauro_model_id')),
            mauroModelLabel: rs.getString('mauro_model_label'),
            label: rs.getString('model_index_label'),
            profileName: rs.getString('profile_name'),
            corpusName: rs.getString('corpus_name'),
            status: rs.getString('status'),
            rebuildEmbeddings: rs.getBoolean('force'),
            maxRows: integerOrNull(rs, 'max_rows'),
            batchSize: integerOrNull(rs, 'batch_size'),
            result: parseJson(rs.getString('result_json')),
            error: rs.getString('error'),
            startedAt: rs.getTimestamp('started_at')?.toInstant()?.toString(),
            completedAt: rs.getTimestamp('completed_at')?.toInstant()?.toString(),
            createdAt: rs.getTimestamp('created_at')?.toInstant()?.toString(),
            updatedAt: rs.getTimestamp('updated_at')?.toInstant()?.toString()
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

    private static void setInteger(PreparedStatement statement, int index, Integer value) {
        if (value == null) {
            statement.setObject(index, null)
        } else {
            statement.setInt(index, value)
        }
    }

    private static Integer integerOrNull(ResultSet rs, String column) {
        int value = rs.getInt(column)
        rs.wasNull() ? null : Integer.valueOf(value)
    }

    private static Object parseJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return [:] as Map<String, Object>
        }
        new JsonSlurper().parseText(json)
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
            targetId: (UUID) rs.getObject('target_id'),
            targetDomainType: rs.getString('target_domain_type'),
            targetLabel: rs.getString('target_label'),
            targetDescription: rs.getString('target_description'),
            relationDistance: rs.getInt('relation_distance'),
            description: rs.getString('description'),
            chunkKind: rs.getString('chunk_kind'),
            chunkGroup: rs.getString('chunk_group'),
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
        addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'label', 0, label, dateCreated, lastUpdated)
        addLabelSubchunks(chunks, corpusId, sourceId, domainType, label, mauroModelId, dateCreated, lastUpdated)
        addIdentifierLabelChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, dateCreated, lastUpdated)
        List<String> descriptionSections = descriptionSections(description)
        for (int i = 0; i < descriptionSections.size(); i++) {
            addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'description-section', 100 + i, descriptionSections.get(i), dateCreated, lastUpdated)
        }
        addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'summary', 2, [domainType, label, description].findAll {String value -> value}.join('. '), dateCreated, lastUpdated)
        chunks
    }

    private static void addLabelSubchunks(List<SemanticChunk> chunks,
                                          UUID corpusId,
                                          UUID sourceId,
                                          String domainType,
                                          String label,
                                          UUID mauroModelId,
                                          Instant dateCreated,
                                          Instant lastUpdated) {
        List<String> words = labelWords(label)
        for (int i = 0; i < words.size(); i++) {
            if (i + 2 < words.size()) {
                addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'label-phrase', 40 + i, "${words.get(i)} ${words.get(i + 1)} ${words.get(i + 2)}".toString(), dateCreated, lastUpdated)
            }
        }
    }

    private static void addIdentifierLabelChunk(List<SemanticChunk> chunks,
                                                UUID corpusId,
                                                UUID sourceId,
                                                String domainType,
                                                String label,
                                                UUID mauroModelId,
                                                Instant dateCreated,
                                                Instant lastUpdated) {
        List<String> words = identifierLabelWords(label)
        if (words.size() >= 2) {
            addChunk(chunks, corpusId, sourceId, domainType, label, mauroModelId, 'label-identifier', 60, words.join(' '), dateCreated, lastUpdated)
        }
    }

    private static List<String> labelWords(String label) {
        if (label == null || label.trim().isEmpty()) {
            return Collections.<String>emptyList()
        }
        label.replaceAll(/[^A-Za-z]+/, ' ')
            .trim()
            .split(/\s+/)
            .findAll {String word -> word ==~ /[A-Za-z]+/ && word.length() >= 4}
            .toList() as List<String>
    }

    private static List<String> identifierLabelWords(String label) {
        if (label == null || label.trim().isEmpty()) {
            return Collections.<String>emptyList()
        }
        label
            .replaceAll(/([A-Z]+)([A-Z][a-z])/, '$1 $2')
            .replaceAll(/([a-z0-9])([A-Z])/, '$1 $2')
            .replaceAll(/([A-Za-z])([0-9])/, '$1 $2')
            .replaceAll(/([0-9])([A-Za-z])/, '$1 $2')
            .replaceAll(/[^A-Za-z0-9]+/, ' ')
            .trim()
            .split(/\s+/)
            .findAll {String word -> word ==~ /[A-Za-z0-9]+/ }
            .collect {String word -> word.toLowerCase(Locale.ROOT)}
            .toList() as List<String>
    }

    private static List<String> descriptionSections(String description) {
        if (description == null || description.trim().isEmpty()) {
            return Collections.<String>emptyList()
        }
        description
            .split(/(?:\r?\n\s*){2,}/)
            .collect {String section -> section.trim()}
            .findAll {String section -> !section.isEmpty()} as List<String>
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
        if (text == null) {
            return
        }
        final String clean = text.trim()
        if (clean.isEmpty()) {
            return
        }
        if (chunks.any {SemanticChunk chunk -> chunk.sourceText == clean}) {
            return
        }
        chunks.add(new SemanticChunk(
            corpusId: corpusId,
            sourceType: 'catalogue-item',
            sourceId: sourceId,
            sourceDomainType: domainType,
            sourceLabel: label,
            mauroModelId: mauroModelId,
            chunkKind: kind,
            chunkGroup: chunkGroupForKind(kind),
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

    private static String modelScopeClause(UUID mauroModelId, String modelIdColumn) {
        if (mauroModelId == null) {
            return ''
        }
        """
            AND ${modelIdColumn} IN (
                WITH RECURSIVE requested_scope(id) AS (
                    SELECT ?::uuid
                ),
                scoped_folders(id) AS (
                    SELECT folder.id
                    FROM core.folder folder
                         JOIN requested_scope scope ON scope.id = folder.id
                    UNION ALL
                    SELECT child.id
                    FROM core.folder child
                         JOIN scoped_folders parent ON child.parent_folder_id = parent.id
                ),
                scoped_model_ids(id) AS (
                    SELECT scope.id
                    FROM requested_scope scope
                    WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
                       OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
                       OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
                    UNION
                    SELECT data_model.id
                    FROM datamodel.data_model data_model
                         JOIN scoped_folders folder ON folder.id = data_model.folder_id
                    UNION
                    SELECT terminology.id
                    FROM terminology.terminology terminology
                         JOIN scoped_folders folder ON folder.id = terminology.folder_id
                    UNION
                    SELECT code_set.id
                    FROM terminology.code_set code_set
                         JOIN scoped_folders folder ON folder.id = code_set.folder_id
                )
                SELECT id FROM scoped_model_ids
            )
        """
    }

    private static String scopedModelIdsSql() {
        """
            WITH RECURSIVE requested_scope(id) AS (
                SELECT ?::uuid
            ),
            scoped_folders(id) AS (
                SELECT folder.id
                FROM core.folder folder
                     JOIN requested_scope scope ON scope.id = folder.id
                UNION ALL
                SELECT child.id
                FROM core.folder child
                     JOIN scoped_folders parent ON child.parent_folder_id = parent.id
            )
            SELECT scope.id
            FROM requested_scope scope
            WHERE EXISTS (SELECT 1 FROM datamodel.data_model data_model WHERE data_model.id = scope.id)
               OR EXISTS (SELECT 1 FROM terminology.terminology terminology WHERE terminology.id = scope.id)
               OR EXISTS (SELECT 1 FROM terminology.code_set code_set WHERE code_set.id = scope.id)
            UNION
            SELECT data_model.id
            FROM datamodel.data_model data_model
                 JOIN scoped_folders folder ON folder.id = data_model.folder_id
            UNION
            SELECT terminology.id
            FROM terminology.terminology terminology
                 JOIN scoped_folders folder ON folder.id = terminology.folder_id
            UNION
            SELECT code_set.id
            FROM terminology.code_set code_set
                 JOIN scoped_folders folder ON folder.id = code_set.folder_id
        """
    }

    private static String catalogueSourceRowsSql(List<String> domainTypes, UUID mauroModelId, Integer maxRows, boolean identifiersOnly = false) {
        String domainClause = domainTypes == null || domainTypes.isEmpty() ? '' : ' AND sd.domain_type = ANY (?::varchar[])'
        String mauroModelClause = modelScopeClause(mauroModelId, 'sd.model_id')
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

    private static String catalogueGeneratedChunksSql(boolean includePayload) {
        String selectColumns = includePayload ?
            "id, domain_type, label, model_id, chunk_kind, ${chunkGroupCaseSql('chunk_kind')} AS chunk_group, chunk_ordinal, btrim(source_text) AS source_text, date_created, last_updated" :
            "id, domain_type, chunk_kind, ${chunkGroupCaseSql('chunk_kind')} AS chunk_group, chunk_ordinal, btrim(source_text) AS source_text"
        """
            SELECT ${selectColumns}
            FROM (
                SELECT generated.*,
                       row_number() OVER (
                           PARTITION BY generated.id, generated.domain_type, btrim(generated.source_text)
                           ORDER BY CASE generated.chunk_kind
                               WHEN 'label' THEN 0
                               WHEN 'label-identifier' THEN 1
                               WHEN 'label-phrase' THEN 2
                               WHEN 'summary' THEN 3
                               ELSE 4
                           END,
                           generated.chunk_ordinal,
                           generated.chunk_kind
                       ) AS duplicate_rank
                FROM (
                    SELECT id,
                           domain_type,
                           label,
                           model_id,
                           'label'::varchar AS chunk_kind,
                           0 AS chunk_ordinal,
                           label AS source_text,
                           date_created,
                           last_updated
                    FROM source_rows

                    UNION ALL
                    SELECT id,
                           domain_type,
                           label,
                           model_id,
                           'summary'::varchar AS chunk_kind,
                           2 AS chunk_ordinal,
                           concat_ws('. ', NULLIF(btrim(label), ''), NULLIF(btrim(description), '')) AS source_text,
                           date_created,
                           last_updated
                    FROM source_rows

                    UNION ALL
                    SELECT id,
                           domain_type,
                           label,
                           model_id,
                           'label-phrase'::varchar AS chunk_kind,
                           40 + (filtered_ordinal - 1)::integer AS chunk_ordinal,
                           concat_ws(' ', word, next_word, next_next_word) AS source_text,
                           date_created,
                           last_updated
                    FROM (
                        SELECT id,
                               domain_type,
                               label,
                               model_id,
                               date_created,
                               last_updated,
                               word,
                               row_number() OVER (PARTITION BY id ORDER BY word_ordinal) AS filtered_ordinal,
                               lead(word) OVER (PARTITION BY id ORDER BY word_ordinal) AS next_word,
                               lead(word, 2) OVER (PARTITION BY id ORDER BY word_ordinal) AS next_next_word
                        FROM source_rows
                             CROSS JOIN LATERAL regexp_split_to_table(regexp_replace(label, '[^A-Za-z]+', ' ', 'g'), '[[:space:]]+') WITH ORDINALITY AS label_word(word, word_ordinal)
                        WHERE word !~ '[^A-Za-z]'
                          AND length(word) >= 4
                    ) label_words
                    WHERE next_word IS NOT NULL
                      AND next_next_word IS NOT NULL

                    UNION ALL
                    SELECT id,
                           domain_type,
                           label,
                           model_id,
                           'label-identifier'::varchar AS chunk_kind,
                           60 AS chunk_ordinal,
                           identifier_text AS source_text,
                           date_created,
                           last_updated
                    FROM (
                        SELECT id,
                               domain_type,
                               label,
                               model_id,
                               date_created,
                               last_updated,
                               lower(
                                   btrim(
                                       regexp_replace(
                                           regexp_replace(
                                               regexp_replace(
                                                   regexp_replace(
                                                       regexp_replace(label, '([[:upper:]]+)([[:upper:]][[:lower:]])', '\\1 \\2', 'g'),
                                                       '([[:lower:][:digit:]])([[:upper:]])',
                                                       '\\1 \\2',
                                                       'g'
                                                   ),
                                                   '([[:alpha:]])([[:digit:]])',
                                                   '\\1 \\2',
                                                   'g'
                                               ),
                                               '([[:digit:]])([[:alpha:]])',
                                               '\\1 \\2',
                                               'g'
                                           ),
                                           '[^[:alnum:]]+',
                                           ' ',
                                           'g'
                                       )
                                   )
                               ) AS identifier_text
                        FROM source_rows
                    ) identifier_labels
                    WHERE identifier_text ~ '[[:alnum:]]+[[:space:]]+[[:alnum:]]+'

                    UNION ALL
                    SELECT id,
                           domain_type,
                           label,
                           model_id,
                           'description-section'::varchar AS chunk_kind,
                           100 + section_ordinal::integer AS chunk_ordinal,
                           section_text AS source_text,
                           date_created,
                           last_updated
                    FROM source_rows
                         CROSS JOIN LATERAL regexp_split_to_table(description, E'(?:\\r?\\n\\s*){2,}') WITH ORDINALITY AS section(section_text, section_ordinal)

                    UNION ALL
                    SELECT sr.id,
                           sr.domain_type,
                           sr.label,
                           sr.model_id,
                           'enumeration-key'::varchar AS chunk_kind,
                           20 AS chunk_ordinal,
                           ev.key AS source_text,
                           sr.date_created,
                           sr.last_updated
                    FROM source_rows sr
                         JOIN datamodel.enumeration_value ev ON ev.id = sr.id
                    WHERE sr.domain_type = 'EnumerationValue'

                    UNION ALL
                    SELECT sr.id,
                           sr.domain_type,
                           sr.label,
                           sr.model_id,
                           'enumeration-value'::varchar AS chunk_kind,
                           21 AS chunk_ordinal,
                           ev.value AS source_text,
                           sr.date_created,
                           sr.last_updated
                    FROM source_rows sr
                         JOIN datamodel.enumeration_value ev ON ev.id = sr.id
                    WHERE sr.domain_type = 'EnumerationValue'

                    UNION ALL
                    SELECT sr.id,
                           sr.domain_type,
                           sr.label,
                           sr.model_id,
                           'enumeration-category'::varchar AS chunk_kind,
                           22 AS chunk_ordinal,
                           ev.category AS source_text,
                           sr.date_created,
                           sr.last_updated
                    FROM source_rows sr
                         JOIN datamodel.enumeration_value ev ON ev.id = sr.id
                    WHERE sr.domain_type = 'EnumerationValue'

                    UNION ALL
                    SELECT sr.id,
                           sr.domain_type,
                           sr.label,
                           sr.model_id,
                           'term-definition'::varchar AS chunk_kind,
                           21 AS chunk_ordinal,
                           term.definition AS source_text,
                           sr.date_created,
                           sr.last_updated
                    FROM source_rows sr
                         JOIN terminology.term term ON term.id = sr.id
                    WHERE sr.domain_type = 'Term'

                    UNION ALL
                    SELECT sr.id,
                           sr.domain_type,
                           sr.label,
                           sr.model_id,
                           context.context_kind AS chunk_kind,
                           2000 + (row_number() OVER (
                               PARTITION BY sr.id, sr.domain_type, context.context_kind
                               ORDER BY context.relationship_depth, context.context_text, context.context_id
                           ))::integer AS chunk_ordinal,
                           context.context_text AS source_text,
                           sr.date_created,
                           GREATEST(sr.last_updated, context.last_updated) AS last_updated
                    FROM source_rows sr
                         JOIN search.administered_item_context context
                           ON context.source_id = sr.id
                          AND context.source_domain_type = sr.domain_type
                          AND context.context_kind NOT IN ('metadata-key-value', 'classification')
                ) generated
                WHERE source_text IS NOT NULL
                  AND btrim(source_text) <> ''
                  AND domain_type NOT IN ('ClassificationScheme', 'Classifier')
            ) generated
            WHERE duplicate_rank = 1
        """
    }

    private static String projectionTargetsSql() {
        """
            SELECT c.source_id AS target_id,
                   c.source_domain_type AS target_domain_type,
                   c.source_label AS target_label,
                   sd.description AS target_description,
                   0 AS relation_distance
            WHERE EXISTS (
                SELECT 1 FROM requested_domain_types requested
                WHERE requested.domain_type = c.source_domain_type
            )

            UNION ALL
            SELECT model_sd.id AS target_id,
                   model_sd.domain_type AS target_domain_type,
                   model_sd.label AS target_label,
                   model_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'DataClass' THEN 1
                       WHEN 'DataElement' THEN 2
                       WHEN 'DataType' THEN 3
                       WHEN 'EnumerationValue' THEN 4
                       WHEN 'Terminology' THEN 4
                       WHEN 'CodeSet' THEN 4
                       WHEN 'Term' THEN 4
                       ELSE 1
                   END AS relation_distance
            FROM search.search_domains model_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'DataModel')
              AND model_sd.domain_type = 'DataModel'
              AND model_sd.id IN (
                  SELECT COALESCE(c.mauro_model_id, sd.model_id)
                  WHERE c.source_domain_type IN ('DataClass', 'DataElement', 'DataType', 'EnumerationValue')
                  UNION
                  SELECT dc.data_model_id
                  FROM datamodel.data_class dc
                  WHERE c.source_domain_type = 'DataClass'
                    AND dc.id = c.source_id
                  UNION
                  SELECT dc.data_model_id
                  FROM datamodel.data_element de
                       JOIN datamodel.data_class dc ON dc.id = de.data_class_id
                  WHERE c.source_domain_type = 'DataElement'
                    AND de.id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'DataType'
                    AND dt.id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM datamodel.enumeration_value ev
                       JOIN datamodel.data_type dt ON dt.id = ev.enumeration_type_id
                  WHERE c.source_domain_type = 'EnumerationValue'
                    AND ev.id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'Terminology'
                    AND dt.model_resource_domain_type = 'Terminology'
                    AND dt.model_resource_id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'CodeSet'
                    AND dt.model_resource_domain_type = 'CodeSet'
                    AND dt.model_resource_id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM terminology.term term
                       JOIN datamodel.data_type dt
                         ON dt.model_resource_domain_type = 'Terminology'
                        AND dt.model_resource_id = term.terminology_id
                  WHERE c.source_domain_type = 'Term'
                    AND term.id = c.source_id
                  UNION
                  SELECT dt.data_model_id
                  FROM terminology.code_set_term cst
                       JOIN datamodel.data_type dt
                         ON dt.model_resource_domain_type = 'CodeSet'
                        AND dt.model_resource_id = cst.code_set_id
                  WHERE c.source_domain_type = 'Term'
                    AND cst.term_id = c.source_id
              )

            UNION ALL
            SELECT class_sd.id AS target_id,
                   class_sd.domain_type AS target_domain_type,
                   class_sd.label AS target_label,
                   class_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'DataElement' THEN 1
                       WHEN 'DataType' THEN 2
                       WHEN 'EnumerationValue' THEN 3
                       WHEN 'Terminology' THEN 3
                       WHEN 'CodeSet' THEN 3
                       WHEN 'Term' THEN 3
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains class_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'DataClass')
              AND class_sd.domain_type = 'DataClass'
              AND class_sd.id IN (
                  SELECT dc.id
                  FROM datamodel.data_class dc
                  WHERE c.source_domain_type = 'DataClass'
                    AND dc.id = c.source_id
                  UNION
                         SELECT de.data_class_id
                         FROM datamodel.data_element de
                         WHERE c.source_domain_type = 'DataElement'
                           AND de.id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.data_class child_dc
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'DataClass'
                           AND child_dc.id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.data_element de
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'DataElement'
                           AND de.id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM datamodel.data_element de
                         WHERE c.source_domain_type = 'DataType'
                           AND de.data_type_id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.data_element de
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'DataType'
                           AND de.data_type_id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM datamodel.enumeration_value ev
                              JOIN datamodel.data_element de ON de.data_type_id = ev.enumeration_type_id
                         WHERE c.source_domain_type = 'EnumerationValue'
                           AND ev.id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.enumeration_value ev
                              JOIN datamodel.data_element de ON de.data_type_id = ev.enumeration_type_id
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'EnumerationValue'
                           AND ev.id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM datamodel.data_type dt
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                         WHERE c.source_domain_type = 'Terminology'
                           AND dt.model_resource_domain_type = 'Terminology'
                           AND dt.model_resource_id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM datamodel.data_type dt
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                         WHERE c.source_domain_type = 'CodeSet'
                           AND dt.model_resource_domain_type = 'CodeSet'
                           AND dt.model_resource_id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM terminology.term term
                              JOIN datamodel.data_type dt
                                ON dt.model_resource_domain_type = 'Terminology'
                               AND dt.model_resource_id = term.terminology_id
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                         WHERE c.source_domain_type = 'Term'
                           AND term.id = c.source_id
                         UNION
                         SELECT de.data_class_id
                         FROM terminology.code_set_term cst
                              JOIN datamodel.data_type dt
                                ON dt.model_resource_domain_type = 'CodeSet'
                               AND dt.model_resource_id = cst.code_set_id
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                         WHERE c.source_domain_type = 'Term'
                           AND cst.term_id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.data_type dt
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'Terminology'
                           AND dt.model_resource_domain_type = 'Terminology'
                           AND dt.model_resource_id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM datamodel.data_type dt
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'CodeSet'
                           AND dt.model_resource_domain_type = 'CodeSet'
                           AND dt.model_resource_id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM terminology.term term
                              JOIN datamodel.data_type dt
                                ON dt.model_resource_domain_type = 'Terminology'
                               AND dt.model_resource_id = term.terminology_id
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'Term'
                           AND term.id = c.source_id
                         UNION
                         SELECT parent_dc.id
                         FROM terminology.code_set_term cst
                              JOIN datamodel.data_type dt
                                ON dt.model_resource_domain_type = 'CodeSet'
                               AND dt.model_resource_id = cst.code_set_id
                              JOIN datamodel.data_element de ON de.data_type_id = dt.id
                              JOIN datamodel.data_class child_dc ON child_dc.id = de.data_class_id
                              JOIN datamodel.data_class parent_dc ON parent_dc.id = child_dc.parent_data_class_id
                         WHERE c.source_domain_type = 'Term'
                           AND cst.term_id = c.source_id
                     )

            UNION ALL
            SELECT element_sd.id AS target_id,
                   element_sd.domain_type AS target_domain_type,
                   element_sd.label AS target_label,
                   element_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'DataType' THEN 1
                       WHEN 'EnumerationValue' THEN 2
                       WHEN 'Terminology' THEN 2
                       WHEN 'CodeSet' THEN 2
                       WHEN 'Term' THEN 2
                       ELSE 0
                   END AS relation_distance
            FROM datamodel.data_element de
                 JOIN search.search_domains element_sd
                   ON element_sd.domain_type = 'DataElement'
                  AND element_sd.id = de.id
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'DataElement')
              AND (
                  (c.source_domain_type = 'DataElement' AND de.id = c.source_id)
                  OR
                  (c.source_domain_type = 'DataType' AND de.data_type_id = c.source_id)
                  OR
                  (c.source_domain_type = 'EnumerationValue' AND de.data_type_id = (
                      SELECT ev.enumeration_type_id
                      FROM datamodel.enumeration_value ev
                      WHERE ev.id = c.source_id
                  ))
                  OR
                  (c.source_domain_type = 'Terminology' AND de.data_type_id IN (
                      SELECT dt.id
                      FROM datamodel.data_type dt
                      WHERE dt.model_resource_domain_type = 'Terminology'
                        AND dt.model_resource_id = c.source_id
                  ))
                  OR
                  (c.source_domain_type = 'CodeSet' AND de.data_type_id IN (
                      SELECT dt.id
                      FROM datamodel.data_type dt
                      WHERE dt.model_resource_domain_type = 'CodeSet'
                        AND dt.model_resource_id = c.source_id
                  ))
                  OR
                  (c.source_domain_type = 'Term' AND de.data_type_id IN (
                      SELECT dt.id
                      FROM terminology.term term
                           JOIN datamodel.data_type dt
                             ON dt.model_resource_domain_type = 'Terminology'
                            AND dt.model_resource_id = term.terminology_id
                      WHERE term.id = c.source_id
                      UNION
                      SELECT dt.id
                      FROM terminology.code_set_term cst
                           JOIN datamodel.data_type dt
                             ON dt.model_resource_domain_type = 'CodeSet'
                            AND dt.model_resource_id = cst.code_set_id
                      WHERE cst.term_id = c.source_id
                  ))
              )

            UNION ALL
            SELECT type_sd.id AS target_id,
                   type_sd.domain_type AS target_domain_type,
                   type_sd.label AS target_label,
                   type_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'DataElement' THEN 1
                       WHEN 'EnumerationValue' THEN 1
                       WHEN 'Terminology' THEN 1
                       WHEN 'CodeSet' THEN 1
                       WHEN 'Term' THEN 1
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains type_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'DataType')
              AND type_sd.domain_type = 'DataType'
              AND type_sd.id IN (
                  SELECT dt.id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'DataType'
                    AND dt.id = c.source_id
                  UNION
                  SELECT de.data_type_id
                  FROM datamodel.data_element de
                  WHERE c.source_domain_type = 'DataElement'
                    AND de.id = c.source_id
                  UNION
                  SELECT ev.enumeration_type_id
                  FROM datamodel.enumeration_value ev
                  WHERE c.source_domain_type = 'EnumerationValue'
                    AND ev.id = c.source_id
                  UNION
                  SELECT dt.id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'Terminology'
                    AND dt.model_resource_domain_type = 'Terminology'
                    AND dt.model_resource_id = c.source_id
                  UNION
                  SELECT dt.id
                  FROM datamodel.data_type dt
                  WHERE c.source_domain_type = 'CodeSet'
                    AND dt.model_resource_domain_type = 'CodeSet'
                    AND dt.model_resource_id = c.source_id
                  UNION
                  SELECT dt.id
                  FROM terminology.term term
                       JOIN datamodel.data_type dt
                         ON dt.model_resource_domain_type = 'Terminology'
                        AND dt.model_resource_id = term.terminology_id
                  WHERE c.source_domain_type = 'Term'
                    AND term.id = c.source_id
                  UNION
                  SELECT dt.id
                  FROM terminology.code_set_term cst
                       JOIN datamodel.data_type dt
                         ON dt.model_resource_domain_type = 'CodeSet'
                        AND dt.model_resource_id = cst.code_set_id
                  WHERE c.source_domain_type = 'Term'
                    AND cst.term_id = c.source_id
              )

            UNION ALL
            SELECT terminology_sd.id AS target_id,
                   terminology_sd.domain_type AS target_domain_type,
                   terminology_sd.label AS target_label,
                   terminology_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'Term' THEN 1
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains terminology_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'Terminology')
              AND terminology_sd.domain_type = 'Terminology'
              AND terminology_sd.id IN (
                  SELECT terminology.id
                  FROM terminology.terminology terminology
                  WHERE c.source_domain_type = 'Terminology'
                    AND terminology.id = c.source_id
                  UNION
                  SELECT term.terminology_id
                  FROM terminology.term term
                  WHERE c.source_domain_type = 'Term'
                    AND term.id = c.source_id
              )

            UNION ALL
            SELECT term_sd.id AS target_id,
                   term_sd.domain_type AS target_domain_type,
                   term_sd.label AS target_label,
                   term_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'CodeSet' THEN 1
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains term_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'Term')
              AND term_sd.domain_type = 'Term'
              AND term_sd.id IN (
                  SELECT term.id
                  FROM terminology.term term
                  WHERE c.source_domain_type = 'Term'
                    AND term.id = c.source_id
                  UNION
                  SELECT cst.term_id
                  FROM terminology.code_set_term cst
                  WHERE c.source_domain_type = 'CodeSet'
                    AND cst.code_set_id = c.source_id
              )

            UNION ALL
            SELECT code_set_sd.id AS target_id,
                   code_set_sd.domain_type AS target_domain_type,
                   code_set_sd.label AS target_label,
                   code_set_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'Term' THEN 1
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains code_set_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'CodeSet')
              AND code_set_sd.domain_type = 'CodeSet'
              AND code_set_sd.id IN (
                  SELECT code_set.id
                  FROM terminology.code_set code_set
                  WHERE c.source_domain_type = 'CodeSet'
                    AND code_set.id = c.source_id
                  UNION
                  SELECT cst.code_set_id
                  FROM terminology.code_set_term cst
                  WHERE c.source_domain_type = 'Term'
                    AND cst.term_id = c.source_id
              )

            UNION ALL
            SELECT folder_sd.id AS target_id,
                   folder_sd.domain_type AS target_domain_type,
                   folder_sd.label AS target_label,
                   folder_sd.description AS target_description,
                   CASE c.source_domain_type
                       WHEN 'DataModel' THEN 1
                       WHEN 'Terminology' THEN 1
                       WHEN 'CodeSet' THEN 1
                       ELSE 0
                   END AS relation_distance
            FROM search.search_domains folder_sd
            WHERE EXISTS (SELECT 1 FROM requested_domain_types requested WHERE requested.domain_type = 'Folder')
              AND folder_sd.domain_type = 'Folder'
              AND folder_sd.id IN (
                  SELECT folder.id
                  FROM core.folder folder
                  WHERE c.source_domain_type = 'Folder'
                    AND folder.id = c.source_id
                  UNION
                  SELECT dm.folder_id
                  FROM datamodel.data_model dm
                  WHERE c.source_domain_type = 'DataModel'
                    AND dm.id = c.source_id
                  UNION
                  SELECT terminology.folder_id
                  FROM terminology.terminology terminology
                  WHERE c.source_domain_type = 'Terminology'
                    AND terminology.id = c.source_id
                  UNION
                  SELECT code_set.folder_id
                  FROM terminology.code_set code_set
                  WHERE c.source_domain_type = 'CodeSet'
                    AND code_set.id = c.source_id
              )
        """
    }

    private static String chunkGroupCaseSql(String chunkKindExpression) {
        """
            CASE
                WHEN ${chunkKindExpression} IN ('metadata-key-value', 'annotation', 'classification')
                  OR ${chunkKindExpression} LIKE 'semantic-link-%'
                    THEN 'context'
                ELSE 'catalogue'
            END
        """
    }

    private static String chunkGroupForKind(String chunkKind) {
        if (chunkKind == null) {
            return 'catalogue'
        }
        (chunkKind in ['metadata-key-value', 'annotation', 'classification'] || chunkKind.startsWith('semantic-link-')) ?
            'context' :
            'catalogue'
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
            chunkGroup: hasColumn(rs, 'chunk_group') ? rs.getString('chunk_group') : chunkGroupForKind(rs.getString('chunk_kind')),
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

    private void executeIndexStatementWithProgress(String indexName, String sql) {
        long start = System.currentTimeMillis()
        ProgressLogger progressLogger = new ProgressLogger(dataSource, indexName)
        Thread progressThread = new Thread(progressLogger, "semantic-index-progress-${indexName}")
        progressThread.daemon = true
        log.info('Creating semantic HNSW index {}', indexName)
        progressThread.start()
        try {
            executeIndexStatement(sql)
            log.info('Created semantic HNSW index {} in {}', indexName, formatDuration(System.currentTimeMillis() - start))
        } finally {
            progressLogger.stop()
            progressThread.interrupt()
            progressThread.join(1000L)
        }
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(Math.round(millis / 1000.0D), 0L)
        long hours = totalSeconds.intdiv(3600)
        long minutes = (totalSeconds % 3600L).intdiv(60)
        long seconds = totalSeconds % 60L
        if (hours > 0L) {
            return String.format('%dh%02dm%02ds', Long.valueOf(hours), Long.valueOf(minutes), Long.valueOf(seconds))
        }
        if (minutes > 0L) {
            return String.format('%dm%02ds', Long.valueOf(minutes), Long.valueOf(seconds))
        }
        return String.format('%ds', Long.valueOf(seconds))
    }

    @CompileStatic
    private static class ProgressLogger implements Runnable {

        private final DataSource dataSource
        private final DataSource rawDataSource
        private final String indexName
        private volatile boolean running = true

        ProgressLogger(DataSource dataSource, String indexName) {
            this.dataSource = dataSource
            this.rawDataSource = rawDataSource(dataSource)
            this.indexName = indexName
        }

        void stop() {
            running = false
        }

        @Override
        void run() {
            while (running) {
                try {
                    Thread.sleep(30000L)
                    if (!running) {
                        return
                    }
                    logProgress()
                } catch (InterruptedException ignored) {
                    return
                } catch (Throwable t) {
                    log.debug('Could not read semantic HNSW index progress for {}', indexName, t)
                }
            }
        }

        private void logProgress() {
            try (Connection connection = rawDataSource.connection;
                 PreparedStatement statement = connection.prepareStatement('''
                     SELECT index_relid::regclass::text AS index_name,
                            relid::regclass::text AS table_name,
                            phase,
                            blocks_done,
                            blocks_total
                     FROM pg_stat_progress_create_index
                     WHERE index_relid::regclass::text = ?
                        OR relid::regclass::text = 'semantic.semantic_embedding'
                     ORDER BY index_relid::regclass::text NULLS LAST
                     LIMIT 1
                 ''')) {
                statement.setString(1, "semantic.${indexName}")
                try (ResultSet rs = statement.executeQuery()) {
                    if (rs.next()) {
                        long blocksDone = rs.getLong('blocks_done')
                        long blocksTotal = rs.getLong('blocks_total')
                        String percent = blocksTotal > 0L ?
                            String.format('%.1f%%', Double.valueOf((blocksDone * 100.0D) / blocksTotal)) :
                            'unknown'
                        log.info(
                            'Semantic HNSW index {} progress: phase={}, blocks={}/{}, percent={}',
                            indexName,
                            rs.getString('phase'),
                            Long.valueOf(blocksDone),
                            Long.valueOf(blocksTotal),
                            percent
                        )
                    } else {
                        log.info('Semantic HNSW index {} progress: waiting for PostgreSQL progress row', indexName)
                    }
                }
            }
        }

        private static DataSource rawDataSource(DataSource dataSource) {
            if (dataSource instanceof HikariDataSource) {
                return dataSource
            }
            try {
                if (dataSource.isWrapperFor(HikariDataSource)) {
                    return dataSource.unwrap(HikariDataSource)
                }
            } catch (Throwable ignored) {
                // Fall through to generic DataSource unwrap below.
            }
            try {
                if (dataSource.isWrapperFor(DataSource)) {
                    return dataSource.unwrap(DataSource)
                }
            } catch (Throwable ignored) {
                // Fall through to the original datasource; this may still require a Micronaut connection context.
            }
            dataSource
        }
    }

    private static String vectorIndexName(EmbeddingProfile profile, String chunkGroup = null) {
        String safeName = (profile.name ?: 'profile')
            .replaceAll('[^A-Za-z0-9]+', '_')
            .replaceAll('^_+', '')
            .replaceAll('_+$', '')
            .toLowerCase()
        if (safeName.length() > 38) {
            safeName = safeName.substring(0, 38)
        }
        String groupSuffix = chunkGroup == null || chunkGroup.trim().isEmpty() ?
            '' :
            "_${chunkGroup.replaceAll('[^A-Za-z0-9]+', '_').replaceAll('^_+', '').replaceAll('_+$', '').toLowerCase()}"
        "semantic_embedding_${safeName}${groupSuffix}_hnsw_idx"
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
