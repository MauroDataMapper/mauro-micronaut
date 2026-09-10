package org.maurodata.plugin.chat.semantic

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import jakarta.inject.Singleton
import io.micronaut.data.connection.jdbc.advice.DelegatingDataSource
import org.maurodata.service.semantic.EmbeddingProfile

import javax.sql.DataSource
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

/** Persistent queue and published-generation state, independent of term embedding jobs. */
@CompileStatic
@Singleton
class SetSemanticIndexRepository {
    private final DataSource dataSource

    SetSemanticIndexRepository(DataSource dataSource) {
        // This repository owns JDBC transaction boundaries and a separate session-level
        // advisory lock. A contextual datasource would require ambient @Connectable
        // advice and could route these independent operations onto the same connection.
        this.dataSource = DelegatingDataSource.unwrapDataSource(dataSource)
    }

    List<Map<String, Object>> discoveryDimensions(String profileName, String corpusName) {
        query("""
            SELECT DISTINCT p.name AS profile, c.name AS corpus
            FROM semantic.set_semantic_embedding e
            JOIN semantic.embedding_profile p ON p.id = e.embedding_profile_id AND p.enabled AND p.distance_metric = 'cosine'
            JOIN semantic.semantic_corpus c ON c.id = e.corpus_id AND c.enabled AND c.api_visible
            WHERE (?::text IS NULL OR p.name = ?) AND (?::text IS NULL OR c.name = ?)
            ORDER BY c.name, p.name
        """, [profileName, profileName, corpusName, corpusName])
    }

    Map<String, Object> resolveSource(String type, UUID id) {
        if (type in ['Terminology', 'CodeSet']) return [id: id, domainType: type]
        List<Map<String, Object>> rows = query("""
            SELECT CASE WHEN dt.domain_type = 'EnumerationType' THEN dt.id ELSE dt.model_resource_id END AS id,
                   CASE WHEN dt.domain_type = 'EnumerationType' THEN 'DataType' ELSE dt.model_resource_domain_type END AS type
            FROM datamodel.data_type dt
            WHERE (dt.id = ? AND ? = 'DataType' OR ? = 'DataElement' AND EXISTS (
                SELECT 1 FROM datamodel.data_element de WHERE de.id = ? AND de.data_type_id = dt.id))
              AND (dt.domain_type = 'EnumerationType' OR dt.domain_type = 'ModelDataType'
                   AND dt.model_resource_domain_type IN ('Terminology', 'CodeSet'))
        """, [id, type, type, id])
        if (!rows) throw new IllegalArgumentException('Source must use an enumeration, Terminology or CodeSet')
        [id: rows.first().id, domainType: rows.first().type]
    }

    /** Destination scope follows consumers, allowing externally owned referenced sets. */
    List<Map<String, Object>> destinations(EmbeddingProfile profile, String corpus, UUID scope, List<String> types) {
        query("""
            WITH RECURSIVE folders(id) AS (
                SELECT id FROM core.folder WHERE id = ?
                UNION SELECT f.id FROM core.folder f JOIN folders p ON f.parent_folder_id = p.id
            ), models(id) AS (
                SELECT ?::uuid
                UNION SELECT id FROM datamodel.data_model WHERE folder_id IN (SELECT id FROM folders)
                UNION SELECT id FROM terminology.terminology WHERE folder_id IN (SELECT id FROM folders)
                UNION SELECT id FROM terminology.code_set WHERE folder_id IN (SELECT id FROM folders)
            ), sets AS (
                SELECT DISTINCT e.set_id, e.set_domain_type, e.set_label, e.mauro_model_id
                FROM semantic.set_semantic_embedding e JOIN semantic.semantic_corpus c ON c.id = e.corpus_id
                WHERE e.embedding_profile_id = ? AND c.name = ?
            ), usages AS (
                SELECT s.set_id, s.set_domain_type, dt.id AS type_id, dt.label AS type_label,
                       dc.data_model_id AS element_model_id, dt.data_model_id, de.id AS element_id, de.label AS element_label, de.data_class_id
                FROM sets s JOIN datamodel.data_type dt ON
                    (s.set_domain_type = 'DataType' AND dt.id = s.set_id AND dt.domain_type = 'EnumerationType') OR
                    (dt.domain_type = 'ModelDataType' AND dt.model_resource_id = s.set_id
                     AND dt.model_resource_domain_type = s.set_domain_type)
                LEFT JOIN datamodel.data_element de ON de.data_type_id = dt.id
                LEFT JOIN datamodel.data_class dc ON dc.id = de.data_class_id
            ), ancestors(set_id, set_domain_type, class_id) AS (
                SELECT set_id, set_domain_type, data_class_id FROM usages WHERE data_class_id IS NOT NULL
                UNION
                SELECT a.set_id, a.set_domain_type, dc.parent_data_class_id
                FROM ancestors a JOIN datamodel.data_class dc ON dc.id = a.class_id
                WHERE dc.parent_data_class_id IS NOT NULL
            ), targets AS (
                SELECT set_id, set_domain_type, set_id AS id, set_domain_type AS domain_type,
                       set_label AS label, mauro_model_id AS model_id FROM sets
                UNION SELECT set_id, set_domain_type, type_id, 'DataType', type_label, data_model_id FROM usages
                UNION SELECT set_id, set_domain_type, element_id, 'DataElement', element_label, element_model_id
                      FROM usages WHERE element_id IS NOT NULL
                UNION SELECT a.set_id, a.set_domain_type, dc.id, 'DataClass', dc.label, dc.data_model_id
                      FROM ancestors a JOIN datamodel.data_class dc ON dc.id = a.class_id
                UNION SELECT u.set_id, u.set_domain_type, dm.id, 'DataModel', dm.label, dm.id
                      FROM usages u JOIN datamodel.data_model dm ON dm.id = u.data_model_id OR dm.id = u.element_model_id
            )
            SELECT DISTINCT set_id, set_domain_type, id, domain_type, label FROM targets
            WHERE (?::uuid IS NULL OR model_id IN (SELECT id FROM models))
              AND (domain_type = ANY (string_to_array(?, ',')))
        """, [scope, scope, profile.id, corpus, scope,
               (types ?: ['Terminology', 'CodeSet', 'DataType']).join(',')])
    }

    void declare(EmbeddingProfile profile, String corpus, UUID modelId, String configuration) {
        execute('''
            INSERT INTO semantic.set_semantic_index_state (corpus_id, embedding_profile_id, mauro_model_id, configuration_fingerprint)
            SELECT id, ?, ?, ? FROM semantic.semantic_corpus WHERE name = ?
            ON CONFLICT (corpus_id, embedding_profile_id, mauro_model_id) DO UPDATE
            SET revision = set_semantic_index_state.revision + 1,
                configuration_fingerprint = EXCLUDED.configuration_fingerprint,
                status = CASE WHEN set_semantic_index_state.status = 'RUNNING' THEN 'RUNNING' ELSE 'QUEUED' END,
                requested_at = now()
            WHERE set_semantic_index_state.configuration_fingerprint IS DISTINCT FROM EXCLUDED.configuration_fingerprint
        ''', [profile.id, modelId, configuration, corpus])
    }

    void request(EmbeddingProfile profile, String corpus, UUID modelId) {
        execute('''
            UPDATE semantic.set_semantic_index_state s SET revision = revision + 1,
                status = CASE WHEN status = 'RUNNING' THEN 'RUNNING' ELSE 'QUEUED' END, requested_at = now(), last_error = NULL, explicit_request = TRUE
            FROM semantic.semantic_corpus c
            WHERE s.corpus_id = c.id AND c.name = ? AND embedding_profile_id = ? AND mauro_model_id = ?
        ''', [corpus, profile.id, modelId])
    }

    Map<String, Object> claim(EmbeddingProfile profile, String corpus, UUID modelId) {
        List<Map<String, Object>> rows = query('''
            UPDATE semantic.set_semantic_index_state s SET status = 'RUNNING', started_at = now(), completed_at = NULL, last_error = NULL
            FROM semantic.semantic_corpus c
            WHERE s.corpus_id = c.id AND c.name = ? AND embedding_profile_id = ? AND mauro_model_id = ? AND status = 'QUEUED'
            RETURNING s.*
        ''', [corpus, profile.id, modelId])
        rows ? rows.first() : null
    }

    Map<String, Object> state(EmbeddingProfile profile, String corpus, UUID modelId) {
        List<Map<String, Object>> rows = query('''
            SELECT s.*, (s.revision <> s.indexed_revision OR COALESCE((s.metadata->>'retainedVectorCount')::int, 0) > 0) AS stale FROM semantic.set_semantic_index_state s
            JOIN semantic.semantic_corpus c ON c.id = s.corpus_id
            WHERE c.name = ? AND s.embedding_profile_id = ? AND s.mauro_model_id = ?
        ''', [corpus, profile.id, modelId])
        rows ? rows.first() : [status: 'NOT_INDEXED', stale: true] as Map<String, Object>
    }

    void failed(Map<String, Object> job, Throwable failure) {
        execute('''
            UPDATE semantic.set_semantic_index_state SET status = CASE WHEN revision <> ? THEN 'QUEUED' ELSE 'FAILED' END,
                completed_at = now(), last_error = ?, explicit_request = (explicit_request AND revision <> ?)
            WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ?
        ''', [job.revision, failure.message ?: failure.class.name, job.revision, job.corpus_id, job.embedding_profile_id, job.mauro_model_id])
    }

    List<Map<String, Object>> queued() {
        query("""
            SELECT s.corpus_id, s.embedding_profile_id, s.mauro_model_id, s.explicit_request, c.name AS corpus_name, p.name AS profile_name
            FROM semantic.set_semantic_index_state s
            JOIN semantic.semantic_corpus c ON c.id = s.corpus_id AND c.enabled
            JOIN semantic.embedding_profile p ON p.id = s.embedding_profile_id AND p.enabled
            WHERE s.status = 'QUEUED' ORDER BY s.requested_at, s.mauro_model_id
        """, [])
    }

    Map<String, Object> withScopeLock(EmbeddingProfile profile, String corpus, UUID modelId, Closure<Map<String, Object>> work) {
        String key = [profile.id, corpus, modelId].join('|')
        try (Connection connection = dataSource.connection) {
            List<Map<String, Object>> lock = query(connection, 'SELECT pg_try_advisory_lock(hashtextextended(?, 0)) AS acquired', [key])
            if (lock.first().acquired != true) return [status: 'RUNNING'] as Map<String, Object>
            try { work.call() }
            finally { query(connection, 'SELECT pg_advisory_unlock(hashtextextended(?, 0))', [key]) }
        }
    }

    void recoverInterrupted() {
        for (Map<String, Object> row : query("""
            SELECT s.*, c.name AS corpus_name FROM semantic.set_semantic_index_state s
            JOIN semantic.semantic_corpus c ON c.id = s.corpus_id WHERE s.status = 'RUNNING'
        """, [])) {
            withScopeLock(new EmbeddingProfile(id: (UUID) row.embedding_profile_id), row.corpus_name.toString(), (UUID) row.mauro_model_id, {
                execute("""
                    UPDATE semantic.set_semantic_index_state SET status = 'QUEUED', completed_at = now(),
                        last_error = 'Derived rebuild interrupted; queued for retry'
                    WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ? AND status = 'RUNNING'
                """, [row.corpus_id, row.embedding_profile_id, row.mauro_model_id])
                [:] as Map<String, Object>
            })
        }
    }

    /** All old rows survive failures; readers see either the old generation or the complete replacement. */
    void publish(Map<String, Object> job, List<SetSemanticCentroid> centroids, Map<String, Object> metadata, boolean incomplete) {
        try (Connection connection = dataSource.connection) {
            connection.autoCommit = false
            try {
                // Serialize with invalidation and check that the owner still exists after a concurrent delete.
                List<Map<String, Object>> locked = query(connection, '''
                    SELECT revision FROM semantic.set_semantic_index_state
                    WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ? FOR UPDATE
                ''', [job.corpus_id, job.embedding_profile_id, job.mauro_model_id])
                if (!locked) { connection.rollback(); return }
                if (locked.first().revision != job.revision) {
                    // Do not republish a snapshot containing an owner/member deleted during calculation.
                    execute(connection, """
                        UPDATE semantic.set_semantic_index_state SET status = 'QUEUED', completed_at = now()
                        WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ?
                    """, [job.corpus_id, job.embedding_profile_id, job.mauro_model_id])
                    connection.commit()
                    return
                }
                Map<String, Object> sets = metadata.sets instanceof Map ? (Map<String, Object>) metadata.sets : [:]
                List<String> incompleteKeys = sets.findAll { String key, Object value -> ((Map) value).incomplete == true }.keySet().toList()
                def incompleteArray = connection.createArrayOf('text', incompleteKeys as String[])
                List<Map<String, Object>> retained = query(connection, """
                    SELECT set_domain_type || '|' || set_id::text || '|' || vector_family AS key, count(*) AS vectors
                    FROM semantic.set_semantic_embedding
                    WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ?
                      AND (set_domain_type || '|' || set_id::text || '|' || vector_family) = ANY (?)
                    GROUP BY set_domain_type, set_id, vector_family
                """, [job.corpus_id, job.embedding_profile_id, job.mauro_model_id, incompleteArray])
                Set<String> retainedKeys = retained.collect { it.key.toString() } as Set<String>
                List<SetSemanticCentroid> replacement = centroids.findAll {
                    !retainedKeys.contains([it.setDomainType, it.setId, it.vectorFamily].join('|'))
                }
                long retainedCount = 0L
                retained.each { Map<String, Object> row ->
                    retainedCount += ((Number) row.vectors).longValue()
                    ((Map) sets.get(row.key.toString())).put('retainedPreviousGeneration', true)
                }
                metadata.put('retainedVectorCount', retainedCount)
                metadata.put('setVectors', replacement.size() + retainedCount)
                execute(connection, """
                    DELETE FROM semantic.set_semantic_embedding
                    WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ?
                      AND NOT ((set_domain_type || '|' || set_id::text || '|' || vector_family) = ANY (?))
                """, [job.corpus_id, job.embedding_profile_id, job.mauro_model_id,
                      connection.createArrayOf('text', retainedKeys as String[])])
                try (PreparedStatement statement = connection.prepareStatement('''
                    INSERT INTO semantic.set_semantic_embedding (
                        corpus_id, embedding_profile_id, set_id, set_domain_type, set_label, mauro_model_id,
                        vector_family, centroid_kind, region_ordinal, member_count, source_member_count,
                        residual_radius, local_radius, source_fingerprint, embedding, metadata)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector, ?::jsonb)
                ''')) {
                    for (SetSemanticCentroid centroid : replacement) {
                        bind(statement, [job.corpus_id, job.embedding_profile_id, centroid.setId, centroid.setDomainType,
                            centroid.setLabel, centroid.mauroModelId, centroid.vectorFamily, centroid.centroidKind,
                            centroid.regionOrdinal, centroid.memberCount, centroid.sourceMemberCount, centroid.residualRadius,
                            centroid.localRadius, centroid.sourceFingerprint, vectorLiteral(centroid.embedding), JsonOutput.toJson(centroid.metadata)])
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                execute(connection, '''
                    UPDATE semantic.set_semantic_index_state SET indexed_revision = ?,
                        status = CASE WHEN revision <> ? THEN 'QUEUED' WHEN ? THEN 'PARTIAL' ELSE 'READY' END,
                        completed_at = now(), last_error = NULL, metadata = ?::jsonb, explicit_request = (explicit_request AND revision <> ?)
                    WHERE corpus_id = ? AND embedding_profile_id = ? AND mauro_model_id = ?
                ''', [job.revision, job.revision, incomplete, JsonOutput.toJson(metadata), job.revision, job.corpus_id, job.embedding_profile_id, job.mauro_model_id])
                connection.commit()
            } catch (Throwable failure) {
                connection.rollback()
                throw failure
            }
        }
    }

    UUID ownerModelId(UUID setId, String domainType) {
        List<Map<String, Object>> owners = query("""
            SELECT id FROM terminology.terminology WHERE id = ? AND ? = 'Terminology'
            UNION ALL SELECT id FROM terminology.code_set WHERE id = ? AND ? = 'CodeSet'
            UNION ALL SELECT data_model_id AS id FROM datamodel.data_type
                WHERE id = ? AND ? = 'DataType' AND domain_type = 'EnumerationType'
        """, [setId, domainType, setId, domainType, setId, domainType])
        owners ? (UUID) owners.first().id : null
    }

    private List<Map<String, Object>> query(String sql, List parameters) {
        try (Connection connection = dataSource.connection) { query(connection, sql, parameters) }
    }

    private static List<Map<String, Object>> query(Connection connection, String sql, List parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters)
            try (ResultSet rs = statement.executeQuery()) {
                List<Map<String, Object>> rows = []
                while (rs.next()) {
                    Map<String, Object> row = [:]
                    for (int i = 1; i <= rs.metaData.columnCount; i++) {
                        String name = rs.metaData.getColumnLabel(i)
                        Object value = rs.getObject(i)
                        row.put(name, name == 'metadata' && value != null ? new JsonSlurper().parseText(value.toString()) : value)
                    }
                    rows.add(row)
                }
                rows
            }
        }
    }

    private void execute(String sql, List parameters) {
        try (Connection connection = dataSource.connection) { execute(connection, sql, parameters) }
    }

    private static void execute(Connection connection, String sql, List parameters) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { bind(statement, parameters); statement.executeUpdate() }
    }

    private static void bind(PreparedStatement statement, List parameters) {
        for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters[i])
    }

    private static String vectorLiteral(float[] vector) { '[' + vector.collect { Float v -> v.toString() }.join(',') + ']' }
}
