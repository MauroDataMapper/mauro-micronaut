package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

import java.time.Instant

@CompileStatic
class SemanticDtoSupport {

    static UUID uuidValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value == null || String.valueOf(value).trim().isEmpty() ? null : UUID.fromString(String.valueOf(value))
    }

    static String stringValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value == null ? null : String.valueOf(value)
    }

    static Integer integerValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value instanceof Number ? ((Number) value).intValue() : (value == null ? null : Integer.valueOf(String.valueOf(value)))
    }

    static Long longValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value instanceof Number ? ((Number) value).longValue() : (value == null ? null : Long.valueOf(String.valueOf(value)))
    }

    static Boolean booleanValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value instanceof Boolean ? (Boolean) value : (value == null ? null : Boolean.valueOf(String.valueOf(value)))
    }

    static Instant instantValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        if (value == null) {
            return null
        }
        if (value instanceof Instant) {
            return (Instant) value
        }
        Instant.parse(String.valueOf(value))
    }

    @SuppressWarnings('unchecked')
    static Map<String, Object> mapValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        value instanceof Map ? new LinkedHashMap<String, Object>((Map<String, Object>) value) : null
    }

    static List<String> stringListValue(Map<String, Object> source, String key) {
        Object value = source == null ? null : source.get(key)
        if (value instanceof Collection) {
            return ((Collection<?>) value).collect {Object item -> String.valueOf(item)} as List<String>
        }
        value == null ? null : String.valueOf(value).split(/\s*,\s*/).toList()
    }
}

@Introspected
@CompileStatic
class SemanticIndexRebuildResponseDTO {
    String indexName
    String corpusName
    String status
    String reason
    Long chunks
    Long embeddings
    Long skippedEmbeddings
    Long reusedEmbeddings
    Integer batchSize
    Boolean force
    Long elapsedMs
    Map<String, Object> profileEmbeddings
    Map<String, Object> profileSkippedEmbeddings
    Map<String, Object> profileReusedEmbeddings

    static SemanticIndexRebuildResponseDTO fromMap(Map<String, Object> source) {
        new SemanticIndexRebuildResponseDTO(
            indexName: SemanticDtoSupport.stringValue(source, 'indexName'),
            corpusName: SemanticDtoSupport.stringValue(source, 'corpusName'),
            status: SemanticDtoSupport.stringValue(source, 'status'),
            reason: SemanticDtoSupport.stringValue(source, 'reason'),
            chunks: SemanticDtoSupport.longValue(source, 'chunks'),
            embeddings: SemanticDtoSupport.longValue(source, 'embeddings'),
            skippedEmbeddings: SemanticDtoSupport.longValue(source, 'skippedEmbeddings'),
            reusedEmbeddings: SemanticDtoSupport.longValue(source, 'reusedEmbeddings'),
            batchSize: SemanticDtoSupport.integerValue(source, 'batchSize'),
            force: SemanticDtoSupport.booleanValue(source, 'force'),
            elapsedMs: SemanticDtoSupport.longValue(source, 'elapsedMs'),
            profileEmbeddings: SemanticDtoSupport.mapValue(source, 'profileEmbeddings'),
            profileSkippedEmbeddings: SemanticDtoSupport.mapValue(source, 'profileSkippedEmbeddings'),
            profileReusedEmbeddings: SemanticDtoSupport.mapValue(source, 'profileReusedEmbeddings')
        )
    }
}

@Introspected
@CompileStatic
class SemanticCorpusRequestDTO {
    String name
    String source = 'api'
    String description
    Boolean enabled = true
    Boolean apiVisible = true
    Boolean apiManageable = true
    Boolean chunkDeleteApiEnabled = false

    Map<String, Object> toMap() {
        [
            name: name,
            source: source,
            description: description,
            enabled: enabled,
            apiVisible: apiVisible,
            apiManageable: apiManageable,
            chunkDeleteApiEnabled: chunkDeleteApiEnabled
        ] as Map<String, Object>
    }
}

@Introspected
@CompileStatic
class SemanticCorpusDTO {
    UUID id
    String name
    String source
    String description
    Boolean enabled
    String origin
    Boolean apiVisible
    Boolean apiManageable
    Boolean chunkDeleteApiEnabled
    Instant createdAt
    Instant updatedAt

    static SemanticCorpusDTO fromMap(Map<String, Object> source) {
        new SemanticCorpusDTO(
            id: SemanticDtoSupport.uuidValue(source, 'id'),
            name: SemanticDtoSupport.stringValue(source, 'name'),
            source: SemanticDtoSupport.stringValue(source, 'source'),
            description: SemanticDtoSupport.stringValue(source, 'description'),
            enabled: SemanticDtoSupport.booleanValue(source, 'enabled'),
            origin: SemanticDtoSupport.stringValue(source, 'origin'),
            apiVisible: SemanticDtoSupport.booleanValue(source, 'apiVisible'),
            apiManageable: SemanticDtoSupport.booleanValue(source, 'apiManageable'),
            chunkDeleteApiEnabled: SemanticDtoSupport.booleanValue(source, 'chunkDeleteApiEnabled'),
            createdAt: SemanticDtoSupport.instantValue(source, 'createdAt'),
            updatedAt: SemanticDtoSupport.instantValue(source, 'updatedAt')
        )
    }
}

@Introspected
@CompileStatic
class SemanticEmbeddingProfileRequestDTO {
    String name
    String provider
    String embeddingModel
    Integer dimension
    String distanceMetric = 'cosine'
    Boolean enabled = false
    String description

    Map<String, Object> toMap() {
        [
            name: name,
            provider: provider,
            embeddingModel: embeddingModel,
            dimension: dimension,
            distanceMetric: distanceMetric,
            enabled: enabled,
            description: description
        ] as Map<String, Object>
    }
}

@Introspected
@CompileStatic
class SemanticEmbeddingProfileDTO {
    UUID id
    String name
    String provider
    String embeddingModel
    Integer dimension
    String distanceMetric
    Boolean enabled
    String description
    Boolean dimensionInferred

    static SemanticEmbeddingProfileDTO fromMap(Map<String, Object> source) {
        new SemanticEmbeddingProfileDTO(
            id: SemanticDtoSupport.uuidValue(source, 'id'),
            name: SemanticDtoSupport.stringValue(source, 'name'),
            provider: SemanticDtoSupport.stringValue(source, 'provider'),
            embeddingModel: SemanticDtoSupport.stringValue(source, 'embeddingModel'),
            dimension: SemanticDtoSupport.integerValue(source, 'dimension'),
            distanceMetric: SemanticDtoSupport.stringValue(source, 'distanceMetric'),
            enabled: SemanticDtoSupport.booleanValue(source, 'enabled'),
            description: SemanticDtoSupport.stringValue(source, 'description'),
            dimensionInferred: SemanticDtoSupport.booleanValue(source, 'dimensionInferred')
        )
    }
}

@Introspected
@CompileStatic
class SemanticEmbeddingModelPullRequestDTO {
    String provider = 'ollama'
    String model
}

@Introspected
@CompileStatic
class SemanticEmbeddingModelPullResponseDTO {
    String provider
    String model
    String status
    String message
    Integer dimension
    Boolean embeddingProbeSucceeded
    String embeddingProbeError

    static SemanticEmbeddingModelPullResponseDTO fromMap(Map<String, Object> source) {
        new SemanticEmbeddingModelPullResponseDTO(
            provider: SemanticDtoSupport.stringValue(source, 'provider'),
            model: SemanticDtoSupport.stringValue(source, 'model'),
            status: SemanticDtoSupport.stringValue(source, 'status'),
            message: SemanticDtoSupport.stringValue(source, 'message'),
            dimension: SemanticDtoSupport.integerValue(source, 'dimension'),
            embeddingProbeSucceeded: SemanticDtoSupport.booleanValue(source, 'embeddingProbeSucceeded'),
            embeddingProbeError: SemanticDtoSupport.stringValue(source, 'embeddingProbeError')
        )
    }
}

@Introspected
@CompileStatic
class SemanticIndexingStatusDTO {
    Boolean enabled
    Instant updatedAt
    Boolean autoReconcile
    Instant autoReconcileUpdatedAt
    String reason
    List<SemanticIndexJobDTO> reconcileResults = []
    List<SemanticIndexJobDTO> cancelledJobs = []

    static SemanticIndexingStatusDTO fromMap(Map<String, Object> source) {
        new SemanticIndexingStatusDTO(
            enabled: SemanticDtoSupport.booleanValue(source, 'enabled'),
            updatedAt: SemanticDtoSupport.instantValue(source, 'updatedAt'),
            autoReconcile: SemanticDtoSupport.booleanValue(source, 'autoReconcile'),
            autoReconcileUpdatedAt: SemanticDtoSupport.instantValue(source, 'autoReconcileUpdatedAt'),
            reason: SemanticDtoSupport.stringValue(source, 'reason'),
            reconcileResults: SemanticIndexJobDTO.listFrom(source == null ? null : source.get('reconcileResults')),
            cancelledJobs: SemanticIndexJobDTO.listFrom(source == null ? null : source.get('cancelledJobs'))
        )
    }
}

@Introspected
@CompileStatic
class SemanticModelIndexRequestDTO {
    UUID modelId
    UUID mauroModelId
    String profileName
    String corpusName
    Boolean enabled = true
    String label

    Map<String, Object> toMap() {
        [
            modelId: modelId,
            mauroModelId: mauroModelId,
            profileName: profileName,
            corpusName: corpusName,
            enabled: enabled,
            label: label
        ] as Map<String, Object>
    }
}

@Introspected
@CompileStatic
class SemanticModelIndexDTO {
    UUID id
    UUID mauroModelId
    String mauroModelLabel
    String label
    String corpusName
    String profileName
    String provider
    String embeddingModel
    Integer dimension
    Boolean enabled
    String status
    String lastError
    Instant lastIndexedAt
    Instant staleRequestedAt
    Instant indexingStartedAt
    Instant lastCheckedAt
    Long chunks
    Long embeddings
    Instant createdAt
    Instant updatedAt
    SemanticIndexJobDTO job

    static SemanticModelIndexDTO fromMap(Map<String, Object> source) {
        new SemanticModelIndexDTO(
            id: SemanticDtoSupport.uuidValue(source, 'id'),
            mauroModelId: SemanticDtoSupport.uuidValue(source, 'mauroModelId'),
            mauroModelLabel: SemanticDtoSupport.stringValue(source, 'mauroModelLabel'),
            label: SemanticDtoSupport.stringValue(source, 'label'),
            corpusName: SemanticDtoSupport.stringValue(source, 'corpusName'),
            profileName: SemanticDtoSupport.stringValue(source, 'profileName'),
            provider: SemanticDtoSupport.stringValue(source, 'provider'),
            embeddingModel: SemanticDtoSupport.stringValue(source, 'embeddingModel'),
            dimension: SemanticDtoSupport.integerValue(source, 'dimension'),
            enabled: SemanticDtoSupport.booleanValue(source, 'enabled'),
            status: SemanticDtoSupport.stringValue(source, 'status'),
            lastError: SemanticDtoSupport.stringValue(source, 'lastError'),
            lastIndexedAt: SemanticDtoSupport.instantValue(source, 'lastIndexedAt'),
            staleRequestedAt: SemanticDtoSupport.instantValue(source, 'staleRequestedAt'),
            indexingStartedAt: SemanticDtoSupport.instantValue(source, 'indexingStartedAt'),
            lastCheckedAt: SemanticDtoSupport.instantValue(source, 'lastCheckedAt'),
            chunks: SemanticDtoSupport.longValue(source, 'chunks'),
            embeddings: SemanticDtoSupport.longValue(source, 'embeddings'),
            createdAt: SemanticDtoSupport.instantValue(source, 'createdAt'),
            updatedAt: SemanticDtoSupport.instantValue(source, 'updatedAt'),
            job: SemanticIndexJobDTO.fromMap(SemanticDtoSupport.mapValue(source, 'job'))
        )
    }

    static List<SemanticModelIndexDTO> listFrom(Object value) {
        if (!(value instanceof Collection)) {
            return []
        }
        ((Collection<?>) value).findAll {Object item -> item instanceof Map || item instanceof SemanticModelIndexDTO}
            .collect {Object item -> item instanceof SemanticModelIndexDTO ? (SemanticModelIndexDTO) item : fromMap((Map<String, Object>) item)} as List<SemanticModelIndexDTO>
    }
}

@Introspected
@CompileStatic
class SemanticModelIndexJobStartRequestDTO {
    Boolean runWhenIndexingDisabled = false
    Boolean rebuildEmbeddings = false
    Integer maxRows
    Integer batchSize

    Map<String, Object> toMap() {
        [
            runWhenIndexingDisabled: runWhenIndexingDisabled,
            rebuildEmbeddings: rebuildEmbeddings,
            maxRows: maxRows,
            batchSize: batchSize
        ] as Map<String, Object>
    }
}

@Introspected
@CompileStatic
class SemanticModelIndexOperationResponseDTO {
    UUID mauroModelId
    String profileName
    String corpusName
    String status
    String reason
    String hint
    Boolean deleteEmbeddings
    Integer matched
    Integer deletedEmbeddings
    List<SemanticIndexJobDTO> jobs = []
    List<Map<String, Object>> deleted = []

    static SemanticModelIndexOperationResponseDTO fromMap(Map<String, Object> source) {
        new SemanticModelIndexOperationResponseDTO(
            mauroModelId: SemanticDtoSupport.uuidValue(source, 'mauroModelId'),
            profileName: SemanticDtoSupport.stringValue(source, 'profileName'),
            corpusName: SemanticDtoSupport.stringValue(source, 'corpusName'),
            status: SemanticDtoSupport.stringValue(source, 'status'),
            reason: SemanticDtoSupport.stringValue(source, 'reason'),
            hint: SemanticDtoSupport.stringValue(source, 'hint'),
            deleteEmbeddings: SemanticDtoSupport.booleanValue(source, 'deleteEmbeddings'),
            matched: SemanticDtoSupport.integerValue(source, 'matched'),
            deletedEmbeddings: SemanticDtoSupport.integerValue(source, 'deletedEmbeddings'),
            jobs: SemanticIndexJobDTO.listFrom(source == null ? null : source.get('jobs')),
            deleted: mapList(source == null ? null : source.get('deleted'))
        )
    }

    @SuppressWarnings('unchecked')
    private static List<Map<String, Object>> mapList(Object value) {
        value instanceof Collection ?
            ((Collection<?>) value).findAll {Object item -> item instanceof Map}.collect {Object item -> new LinkedHashMap<String, Object>((Map<String, Object>) item)} as List<Map<String, Object>> :
            []
    }
}

@Introspected
@CompileStatic
class SemanticIndexJobDTO {
    UUID jobId
    UUID mauroModelId
    String mauroModelLabel
    String label
    String profileName
    String corpusName
    String status
    Boolean rebuildEmbeddings
    Boolean accepted
    String source
    Boolean staleDetected
    Integer maxRows
    Integer batchSize
    Map<String, Object> result
    String error
    Instant startedAt
    Instant completedAt
    Instant createdAt
    Instant updatedAt

    static SemanticIndexJobDTO fromMap(Map<String, Object> source) {
        if (source == null) {
            return null
        }
        new SemanticIndexJobDTO(
            jobId: SemanticDtoSupport.uuidValue(source, 'jobId'),
            mauroModelId: SemanticDtoSupport.uuidValue(source, 'mauroModelId'),
            mauroModelLabel: SemanticDtoSupport.stringValue(source, 'mauroModelLabel'),
            label: SemanticDtoSupport.stringValue(source, 'label'),
            profileName: SemanticDtoSupport.stringValue(source, 'profileName'),
            corpusName: SemanticDtoSupport.stringValue(source, 'corpusName'),
            status: SemanticDtoSupport.stringValue(source, 'status'),
            rebuildEmbeddings: SemanticDtoSupport.booleanValue(source, 'rebuildEmbeddings'),
            accepted: SemanticDtoSupport.booleanValue(source, 'accepted'),
            source: SemanticDtoSupport.stringValue(source, 'source'),
            staleDetected: SemanticDtoSupport.booleanValue(source, 'staleDetected'),
            maxRows: SemanticDtoSupport.integerValue(source, 'maxRows'),
            batchSize: SemanticDtoSupport.integerValue(source, 'batchSize'),
            result: SemanticDtoSupport.mapValue(source, 'result'),
            error: SemanticDtoSupport.stringValue(source, 'error'),
            startedAt: SemanticDtoSupport.instantValue(source, 'startedAt'),
            completedAt: SemanticDtoSupport.instantValue(source, 'completedAt'),
            createdAt: SemanticDtoSupport.instantValue(source, 'createdAt'),
            updatedAt: SemanticDtoSupport.instantValue(source, 'updatedAt')
        )
    }

    static List<SemanticIndexJobDTO> listFrom(Object value) {
        if (!(value instanceof Collection)) {
            return []
        }
        ((Collection<?>) value).findAll {Object item -> item instanceof Map || item instanceof SemanticIndexJobDTO}
            .collect {Object item -> item instanceof SemanticIndexJobDTO ? (SemanticIndexJobDTO) item : fromMap((Map<String, Object>) item)} as List<SemanticIndexJobDTO>
    }
}
