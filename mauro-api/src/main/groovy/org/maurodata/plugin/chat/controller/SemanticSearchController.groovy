package org.maurodata.plugin.chat.controller

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import jakarta.inject.Inject
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.plugin.chat.api.Paths
import org.maurodata.plugin.chat.api.search.SemanticSearchApi
import org.maurodata.security.AccessControlService
import org.maurodata.service.search.SemanticSearchService
import org.maurodata.service.semantic.SemanticEmbeddingModelAdministration
import org.maurodata.service.semantic.SemanticIndexAdministrationService
import org.maurodata.service.semantic.SemanticProfileAdministrationService
import org.maurodata.web.ListResponse
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SemanticSearchController implements AdministeredItemReader, SemanticSearchApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    SemanticSearchService semanticSearchService

    @Inject
    SemanticIndexAdministrationService semanticIndexAdministrationService

    @Inject
    SemanticProfileAdministrationService semanticProfileAdministrationService

    @Inject
    SemanticEmbeddingModelAdministration semanticEmbeddingModelAdministration

    @Audit
    @Operation(summary = "List semantic search results", description = "Returns catalogue items ranked by semantic similarity.")
    @Get(Paths.SEARCH_SEMANTIC_GET)
    ListResponse<SemanticSearchResultsDTO> semanticSearchGet(@RequestBean SemanticSearchRequestDTO requestDTO) {
        executeSemanticSearch(requestDTO)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "List semantic search results", description = "Returns catalogue items ranked by semantic similarity.")
    @Post(Paths.SEARCH_SEMANTIC_POST)
    ListResponse<SemanticSearchResultsDTO> semanticSearchPost(@Body SemanticSearchRequestDTO requestDTO) {
        executeSemanticSearch(requestDTO)
    }

    private ListResponse<SemanticSearchResultsDTO> executeSemanticSearch(SemanticSearchRequestDTO requestDTO) {
        semanticSearchService.executeSearch(
            requestDTO,
            { String domainType, UUID id -> findAdministeredItem(domainType, id) }
        )
    }

    @Audit
    @Post(Paths.SEARCH_REBUILD_SEMANTIC_INDEXES)
    Map<String, Object> rebuildSemanticIndexes(@Body @Nullable Map<String, Object> request) {
        accessControlService.checkAdministrator()
        log.warn("Rebuild semantic index API endpoint called - ordinarily this should be for testing purposes only")
        semanticIndexAdministrationService.rebuildCatalogueIndex(
            stringValue(request, 'indexName') ?: 'catalogue-items-default',
            stringValue(request, 'corpusName') ?: 'catalogue-items',
            stringList(request == null ? null : request.get('domainTypes')),
            uuidValue(request, 'mauroModelId'),
            integerValue(request, 'maxRows'),
            integerValue(request, 'batchSize'),
            booleanValue(request, 'rebuildEmbeddings', false)
        )
    }

    @Audit
    @Get(Paths.SEMANTIC_CORPORA)
    List<Map<String, Object>> semanticCorpora() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.corpora()
    }

    @Audit
    @Post(Paths.SEMANTIC_CORPORA)
    Map<String, Object> createSemanticCorpus(@Body Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.createCorpus(request)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEXES)
    List<Map<String, Object>> semanticIndexes() {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.indexes()
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXES)
    Map<String, Object> createSemanticIndex(@Body Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.createIndex(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX)
    Map<String, Object> deleteSemanticIndex(@PathVariable String indexName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteIndex(indexName)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX_EMBEDDINGS)
    Map<String, Object> deleteSemanticIndexEmbeddings(@PathVariable String indexName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteEmbeddingsForIndex(indexName)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX_CORPUS_CHUNKS)
    Map<String, Object> deleteSemanticCorpusChunks(@PathVariable String corpusName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteChunksForCorpus(corpusName)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_PROFILES)
    List<Map<String, Object>> semanticIndexProfiles() {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.profiles()
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILES)
    Map<String, Object> createSemanticIndexProfile(@Body Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.createProfile(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX_PROFILE)
    Map<String, Object> deleteSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteProfile(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_ENABLE)
    Map<String, Object> enableSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.enable(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_DISABLE)
    Map<String, Object> disableSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.disable(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_LINK)
    Map<String, Object> linkSemanticIndexProfile(@PathVariable String indexName, @PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.link(indexName, profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_UNLINK)
    Map<String, Object> unlinkSemanticIndexProfile(@PathVariable String indexName, @PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.unlink(indexName, profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_EMBEDDING_MODEL_PULL)
    Map<String, Object> pullEmbeddingModel(@Body Map<String, Object> request) {
        accessControlService.checkAdministrator()
        Object providerValue = request == null ? null : request.get('provider')
        Object modelValue = request == null ? null : request.get('model')
        String provider = providerValue == null ? null : String.valueOf(providerValue)
        String model = modelValue == null ? null : String.valueOf(modelValue)
        semanticEmbeddingModelAdministration.pull(provider, model)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEXING_STATUS)
    Map<String, Object> semanticIndexingStatus() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.indexingStatus()
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_ENABLE)
    Map<String, Object> enableSemanticIndexing() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setIndexingEnabled(true)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_DISABLE)
    Map<String, Object> disableSemanticIndexing() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setIndexingEnabled(false)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_ENABLE)
    Map<String, Object> enableSemanticIndexingAutoReconcile() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setAutoReconcileEnabled(true)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_DISABLE)
    Map<String, Object> disableSemanticIndexingAutoReconcile() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setAutoReconcileEnabled(false)
    }

    @Audit
    @Get(Paths.SEMANTIC_MODEL_INDEXES)
    List<Map<String, Object>> semanticModelIndexes() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.modelIndexes()
    }

    @Audit
    @Get(Paths.SEMANTIC_MODEL_INDEX_STATS)
    List<Map<String, Object>> semanticModelIndexStats(@PathVariable UUID modelId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.modelIndexStats(modelId)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEXES)
    Map<String, Object> createSemanticModelIndex(@Body Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.createModelIndex(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX)
    Map<String, Object> deleteSemanticModelIndex(@PathVariable UUID modelId, @PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, profileName)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX_DEFAULT_PROFILE)
    Map<String, Object> deleteDefaultSemanticModelIndex(@PathVariable UUID modelId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, null)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEX_START)
    Map<String, Object> startSemanticModelIndex(@PathVariable UUID modelId,
                                                @PathVariable String profileName,
                                                @Body @Nullable Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJob(modelId, profileName, request)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEX_START_DEFAULT_PROFILE)
    Map<String, Object> startDefaultSemanticModelIndex(@PathVariable UUID modelId,
                                                       @Body @Nullable Map<String, Object> request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJob(modelId, null, request)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_JOBS)
    List<Map<String, Object>> semanticIndexJobs(@QueryValue(defaultValue = 'false') Boolean includeHistory) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.jobs(includeHistory == true)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_JOB)
    Map<String, Object> semanticIndexJob(@PathVariable UUID jobId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.jobStatus(jobId)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_JOB_CANCEL)
    Map<String, Object> cancelSemanticIndexJob(@PathVariable UUID jobId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.cancelJob(jobId)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_JOB_RESUME)
    Map<String, Object> resumeSemanticIndexJob(@PathVariable UUID jobId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.resumeJob(jobId)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_JOB_EVENTS)
    @Produces('application/x-ndjson')
    Publisher<String> semanticIndexJobEvents(@PathVariable UUID jobId,
                                             @QueryValue(defaultValue = 'false') Boolean follow,
                                             @QueryValue(defaultValue = '0') Long after) {
        accessControlService.checkAdministrator()
        Boolean.TRUE.equals(follow) ?
            semanticIndexAdministrationService.followJobEvents(jobId, after) :
            Flux.just(semanticIndexAdministrationService.jobEvents(jobId))
    }

    private static String stringValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        value == null ? null : String.valueOf(value)
    }

    private static Integer integerValue(Map<String, Object> request, String key) {
        Object value = request == null ? null : request.get(key)
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return null
        }
        value instanceof Number ? ((Number) value).intValue() : Integer.valueOf(String.valueOf(value))
    }

    private static UUID uuidValue(Map<String, Object> request, String key) {
        String value = stringValue(request, key)
        value == null || value.trim().isEmpty() ? null : UUID.fromString(value)
    }

    private static List<String> stringList(Object value) {
        if (value instanceof Collection) {
            return ((Collection<?>) value).collect {Object item -> String.valueOf(item)}.findAll {String item -> item?.trim()} as List<String>
        }
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return []
        }
        String.valueOf(value).split(/\s*,\s*/).findAll {String item -> item?.trim()} as List<String>
    }

    private static boolean booleanValue(Map<String, Object> request, String key, boolean fallback) {
        Object value = request == null ? null : request.get(key)
        value == null ? fallback : Boolean.valueOf(String.valueOf(value))
    }

}
