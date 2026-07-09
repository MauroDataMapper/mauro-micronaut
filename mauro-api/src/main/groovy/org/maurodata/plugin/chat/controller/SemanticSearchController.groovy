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
import org.maurodata.domain.search.dto.SemanticCorpusDTO
import org.maurodata.domain.search.dto.SemanticCorpusRequestDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingModelPullRequestDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingModelPullResponseDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileDTO
import org.maurodata.domain.search.dto.SemanticEmbeddingProfileRequestDTO
import org.maurodata.domain.search.dto.SemanticIndexJobDTO
import org.maurodata.domain.search.dto.SemanticIndexingStatusDTO
import org.maurodata.domain.search.dto.SemanticModelIndexDTO
import org.maurodata.domain.search.dto.SemanticModelIndexJobStartRequestDTO
import org.maurodata.domain.search.dto.SemanticModelIndexOperationResponseDTO
import org.maurodata.domain.search.dto.SemanticModelIndexRequestDTO
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
    @Get(Paths.SEMANTIC_CORPORA)
    List<SemanticCorpusDTO> semanticCorpora() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.corpora()
    }

    @Audit
    @Post(Paths.SEMANTIC_CORPORA)
    SemanticCorpusDTO createSemanticCorpus(@Body SemanticCorpusRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.createCorpus(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX_CORPUS_CHUNKS)
    SemanticCorpusDTO deleteSemanticCorpusChunks(@PathVariable String corpusName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteChunksForCorpus(corpusName)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_PROFILES)
    List<SemanticEmbeddingProfileDTO> semanticIndexProfiles() {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.profiles()
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILES)
    SemanticEmbeddingProfileDTO createSemanticIndexProfile(@Body SemanticEmbeddingProfileRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.createProfile(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_INDEX_PROFILE)
    SemanticEmbeddingProfileDTO deleteSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.deleteProfile(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_ENABLE)
    SemanticEmbeddingProfileDTO enableSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.enable(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_PROFILE_DISABLE)
    SemanticEmbeddingProfileDTO disableSemanticIndexProfile(@PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticProfileAdministrationService.disable(profileName)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_EMBEDDING_MODEL_PULL)
    SemanticEmbeddingModelPullResponseDTO pullEmbeddingModel(@Body SemanticEmbeddingModelPullRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticEmbeddingModelAdministration.pull(request?.provider, request?.model)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEXING_STATUS)
    SemanticIndexingStatusDTO semanticIndexingStatus() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.indexingStatus()
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_ENABLE)
    SemanticIndexingStatusDTO enableSemanticIndexing() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setIndexingEnabled(true)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_DISABLE)
    SemanticIndexingStatusDTO disableSemanticIndexing() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setIndexingEnabled(false)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_ENABLE)
    SemanticIndexingStatusDTO enableSemanticIndexingAutoReconcile() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setAutoReconcileEnabled(true)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_DISABLE)
    SemanticIndexingStatusDTO disableSemanticIndexingAutoReconcile() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.setAutoReconcileEnabled(false)
    }

    @Audit
    @Get(Paths.SEMANTIC_MODEL_INDEXES)
    List<SemanticModelIndexDTO> semanticModelIndexes() {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.modelIndexes()
    }

    @Audit
    @Get(Paths.SEMANTIC_MODEL_INDEX_STATS)
    List<SemanticModelIndexDTO> semanticModelIndexStats(@PathVariable UUID modelId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.modelIndexStats(modelId)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEXES)
    SemanticModelIndexDTO createSemanticModelIndex(@Body SemanticModelIndexRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.createModelIndex(request)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForModel(@PathVariable UUID modelId,
                                                           @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, null, null, deleteEmbeddings == true)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForModelProfile(@PathVariable UUID modelId,
                                                                  @PathVariable String profileName,
                                                                  @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, profileName, null, deleteEmbeddings == true)
    }

    @Audit
    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForCorpusModel(@PathVariable String corpusName,
                                                                 @PathVariable UUID modelId,
                                                                 @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, null, corpusName, deleteEmbeddings == true)
    }

    @Audit
    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndex(@PathVariable String corpusName,
                                                 @PathVariable UUID modelId,
                                                 @PathVariable String profileName,
                                                 @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndex(modelId, profileName, corpusName, deleteEmbeddings == true)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForModel(@PathVariable UUID modelId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndexEmbeddings(modelId, null, null)
    }

    @Audit
    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForModelProfile(@PathVariable UUID modelId,
                                                                          @PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndexEmbeddings(modelId, profileName, null)
    }

    @Audit
    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForCorpusModel(@PathVariable String corpusName,
                                                                         @PathVariable UUID modelId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndexEmbeddings(modelId, null, corpusName)
    }

    @Audit
    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddings(@PathVariable String corpusName,
                                                           @PathVariable UUID modelId,
                                                           @PathVariable String profileName) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.deleteModelIndexEmbeddings(modelId, profileName, corpusName)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEX_MODEL_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForModel(@PathVariable UUID modelId,
                                                                            @Body @Nullable SemanticModelIndexJobStartRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJobs(modelId, null, null, request)
    }

    @Audit
    @Post(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForModelProfile(@PathVariable UUID modelId,
                                                                 @PathVariable String profileName,
                                                                 @Body @Nullable SemanticModelIndexJobStartRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJobs(modelId, profileName, null, request)
    }

    @Audit
    @Post(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForCorpusModel(@PathVariable String corpusName,
                                                                @PathVariable UUID modelId,
                                                                @Body @Nullable SemanticModelIndexJobStartRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJobs(modelId, null, corpusName, request)
    }

    @Audit
    @Post(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndex(@PathVariable String corpusName,
                                                @PathVariable UUID modelId,
                                                @PathVariable String profileName,
                                                @Body @Nullable SemanticModelIndexJobStartRequestDTO request) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.startModelIndexJobs(modelId, profileName, corpusName, request)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_JOBS)
    List<SemanticIndexJobDTO> semanticIndexJobs(@QueryValue(defaultValue = 'false') Boolean includeHistory) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.jobs(includeHistory == true)
    }

    @Audit
    @Get(Paths.SEMANTIC_INDEX_JOB)
    SemanticIndexJobDTO semanticIndexJob(@PathVariable UUID jobId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.jobStatus(jobId)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_JOB_CANCEL)
    SemanticIndexJobDTO cancelSemanticIndexJob(@PathVariable UUID jobId) {
        accessControlService.checkAdministrator()
        semanticIndexAdministrationService.cancelJob(jobId)
    }

    @Audit
    @Post(Paths.SEMANTIC_INDEX_JOB_RESUME)
    SemanticIndexJobDTO resumeSemanticIndexJob(@PathVariable UUID jobId) {
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

}
