package org.maurodata.plugin.chat.api.search

import org.maurodata.api.MauroApi
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
import org.maurodata.web.ListResponse

import io.micronaut.context.annotation.Parameter
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.QueryValue
import org.reactivestreams.Publisher

@MauroApi
interface SemanticSearchApi {

    @Get(Paths.SEARCH_SEMANTIC_GET)
    ListResponse<SemanticSearchResultsDTO> semanticSearchGet(@Parameter SemanticSearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_SEMANTIC_POST)
    ListResponse<SemanticSearchResultsDTO> semanticSearchPost(@Body SemanticSearchRequestDTO requestDTO)

    @Get(Paths.SEMANTIC_CORPORA)
    List<SemanticCorpusDTO> semanticCorpora()

    @Post(Paths.SEMANTIC_CORPORA)
    SemanticCorpusDTO createSemanticCorpus(@Body SemanticCorpusRequestDTO request)

    @Delete(Paths.SEMANTIC_INDEX_CORPUS_CHUNKS)
    SemanticCorpusDTO deleteSemanticCorpusChunks(@PathVariable String corpusName)

    @Get(Paths.SEMANTIC_INDEX_PROFILES)
    List<SemanticEmbeddingProfileDTO> semanticIndexProfiles()

    @Post(Paths.SEMANTIC_INDEX_PROFILES)
    SemanticEmbeddingProfileDTO createSemanticIndexProfile(@Body SemanticEmbeddingProfileRequestDTO request)

    @Delete(Paths.SEMANTIC_INDEX_PROFILE)
    SemanticEmbeddingProfileDTO deleteSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_ENABLE)
    SemanticEmbeddingProfileDTO enableSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_DISABLE)
    SemanticEmbeddingProfileDTO disableSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_EMBEDDING_MODEL_PULL)
    SemanticEmbeddingModelPullResponseDTO pullEmbeddingModel(@Body SemanticEmbeddingModelPullRequestDTO request)

    @Get(Paths.SEMANTIC_INDEXING_STATUS)
    SemanticIndexingStatusDTO semanticIndexingStatus()

    @Post(Paths.SEMANTIC_INDEXING_ENABLE)
    SemanticIndexingStatusDTO enableSemanticIndexing()

    @Post(Paths.SEMANTIC_INDEXING_DISABLE)
    SemanticIndexingStatusDTO disableSemanticIndexing()

    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_ENABLE)
    SemanticIndexingStatusDTO enableSemanticIndexingAutoReconcile()

    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_DISABLE)
    SemanticIndexingStatusDTO disableSemanticIndexingAutoReconcile()

    @Get(Paths.SEMANTIC_MODEL_INDEXES)
    List<SemanticModelIndexDTO> semanticModelIndexes()

    @Get(Paths.SEMANTIC_MODEL_INDEX_STATS)
    List<SemanticModelIndexDTO> semanticModelIndexStats(@PathVariable UUID modelId)

    @Post(Paths.SEMANTIC_MODEL_INDEXES)
    SemanticModelIndexDTO createSemanticModelIndex(@Body SemanticModelIndexRequestDTO request)

    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForModel(@PathVariable UUID modelId,
                                                           @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings)

    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForModelProfile(@PathVariable UUID modelId,
                                                                  @PathVariable String profileName,
                                                                  @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings)

    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexesForCorpusModel(@PathVariable String corpusName,
                                                                 @PathVariable UUID modelId,
                                                                 @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings)

    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndex(@PathVariable String corpusName,
                                                 @PathVariable UUID modelId,
                                                 @PathVariable String profileName,
                                                 @QueryValue(defaultValue = 'false') Boolean deleteEmbeddings)

    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForModel(@PathVariable UUID modelId)

    @Delete(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForModelProfile(@PathVariable UUID modelId,
                                                                          @PathVariable String profileName)

    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddingsForCorpusModel(@PathVariable String corpusName,
                                                                         @PathVariable UUID modelId)

    @Delete(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE_EMBEDDINGS)
    SemanticModelIndexOperationResponseDTO deleteSemanticModelIndexEmbeddings(@PathVariable String corpusName,
                                                           @PathVariable UUID modelId,
                                                           @PathVariable String profileName)

    @Post(Paths.SEMANTIC_MODEL_INDEX_MODEL_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForModel(@PathVariable UUID modelId, @Body SemanticModelIndexJobStartRequestDTO request)

    @Post(Paths.SEMANTIC_MODEL_INDEX_MODEL_PROFILE_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForModelProfile(@PathVariable UUID modelId,
                                                                 @PathVariable String profileName,
                                                                 @Body SemanticModelIndexJobStartRequestDTO request)

    @Post(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndexesForCorpusModel(@PathVariable String corpusName,
                                                                @PathVariable UUID modelId,
                                                                @Body SemanticModelIndexJobStartRequestDTO request)

    @Post(Paths.SEMANTIC_CORPUS_MODEL_INDEX_MODEL_PROFILE_START)
    SemanticModelIndexOperationResponseDTO startSemanticModelIndex(@PathVariable String corpusName,
                                                @PathVariable UUID modelId,
                                                @PathVariable String profileName,
                                                @Body SemanticModelIndexJobStartRequestDTO request)

    @Get(Paths.SEMANTIC_INDEX_JOBS)
    List<SemanticIndexJobDTO> semanticIndexJobs(@QueryValue(defaultValue = 'false') Boolean includeHistory)

    @Get(Paths.SEMANTIC_INDEX_JOB)
    SemanticIndexJobDTO semanticIndexJob(@PathVariable UUID jobId)

    @Post(Paths.SEMANTIC_INDEX_JOB_CANCEL)
    SemanticIndexJobDTO cancelSemanticIndexJob(@PathVariable UUID jobId)

    @Post(Paths.SEMANTIC_INDEX_JOB_RESUME)
    SemanticIndexJobDTO resumeSemanticIndexJob(@PathVariable UUID jobId)

    @Get(Paths.SEMANTIC_INDEX_JOB_EVENTS)
    @Produces('application/x-ndjson')
    Publisher<String> semanticIndexJobEvents(@PathVariable UUID jobId,
                                             @QueryValue(defaultValue = 'false') Boolean follow,
                                             @QueryValue(defaultValue = '0') Long after)

}
