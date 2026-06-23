package org.maurodata.controller.search

import io.micronaut.core.annotation.Nullable
import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.search.SearchApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.persistence.search.SearchIndexRefreshScheduler
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.security.AccessControlService
import org.maurodata.service.search.HybridSearchExecutionService
import org.maurodata.service.search.SearchExecutionService
import org.maurodata.service.search.SemanticSearchService
import org.maurodata.service.semantic.SemanticEmbeddingModelAdministration
import org.maurodata.service.semantic.SemanticIndexAdministrationService
import org.maurodata.service.semantic.SemanticProfileAdministrationService
import org.maurodata.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SearchController implements AdministeredItemReader, SearchApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    SearchIndexRefreshScheduler searchIndexRefreshScheduler

    @Inject
    SearchExecutionService searchExecutionService

    @Inject
    HybridSearchExecutionService hybridSearchExecutionService

    @Inject
    SemanticSearchService semanticSearchService

    @Inject
    SemanticIndexAdministrationService semanticIndexAdministrationService

    @Inject
    SemanticProfileAdministrationService semanticProfileAdministrationService

    @Inject
    SemanticEmbeddingModelAdministration semanticEmbeddingModelAdministration

    @Audit
    @Operation(summary = "List the searches", description = "Returns the searches. You must have read privileges on the item in question.")
    @Get(Paths.SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO) {
        executeHybridSearch(requestDTO)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "List the searches", description = "Returns the searches. You must have read privileges on the item in question.")
    @Post(Paths.SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO) {
        executeHybridSearch(requestDTO)
    }

    @Audit
    @Operation(summary = "List keyword search results", description = "Returns keyword/full-text search results. You must have read privileges on the item in question.")
    @Get(Paths.SEARCH_KEYWORD_GET)
    ListResponse<SearchResultsDTO> keywordSearchGet(@RequestBean SearchRequestDTO requestDTO) {
        executeSearch(requestDTO)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Operation(summary = "List keyword search results", description = "Returns keyword/full-text search results. You must have read privileges on the item in question.")
    @Post(Paths.SEARCH_KEYWORD_POST)
    ListResponse<SearchResultsDTO> keywordSearchPost(@Body SearchRequestDTO requestDTO) {
        executeSearch(requestDTO)
    }

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

    private ListResponse<SearchResultsDTO> executeSearch(SearchRequestDTO requestDTO) {
        searchExecutionService.executeSearch(
            requestDTO,
            { String domainType, UUID id -> findAdministeredItem(domainType, id) }
        )
    }

    private ListResponse<SearchResultsDTO> executeHybridSearch(SearchRequestDTO requestDTO) {
        hybridSearchExecutionService.executeSearch(
            requestDTO,
            { String domainType, UUID id -> findAdministeredItem(domainType, id) }
        )
    }

    private ListResponse<SemanticSearchResultsDTO> executeSemanticSearch(SemanticSearchRequestDTO requestDTO) {
        semanticSearchService.executeSearch(
            requestDTO,
            { String domainType, UUID id -> findAdministeredItem(domainType, id) }
        )
    }

    @Audit
    @Post(Paths.SEARCH_REBUILD_INDEXES)
    boolean rebuildIndexes() {
        accessControlService.checkAdministrator()
        log.warn("Rebuild index API endpoint called - ordinarily this should be for testing purposes only")
        searchIndexRefreshScheduler.refreshMaterializedViews()
        return true
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
            booleanValue(request, 'force', false)
        )
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
