package org.maurodata.plugin.chat.api.search

import org.maurodata.api.MauroApi
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
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

    @Post(Paths.SEARCH_REBUILD_SEMANTIC_INDEXES)
    Map<String, Object> rebuildSemanticIndexes(@Body Map<String, Object> request)

    @Get(Paths.SEMANTIC_CORPORA)
    List<Map<String, Object>> semanticCorpora()

    @Post(Paths.SEMANTIC_CORPORA)
    Map<String, Object> createSemanticCorpus(@Body Map<String, Object> request)

    @Get(Paths.SEMANTIC_INDEXES)
    List<Map<String, Object>> semanticIndexes()

    @Post(Paths.SEMANTIC_INDEXES)
    Map<String, Object> createSemanticIndex(@Body Map<String, Object> request)

    @Delete(Paths.SEMANTIC_INDEX)
    Map<String, Object> deleteSemanticIndex(@PathVariable String indexName)

    @Delete(Paths.SEMANTIC_INDEX_EMBEDDINGS)
    Map<String, Object> deleteSemanticIndexEmbeddings(@PathVariable String indexName)

    @Delete(Paths.SEMANTIC_INDEX_CORPUS_CHUNKS)
    Map<String, Object> deleteSemanticCorpusChunks(@PathVariable String corpusName)

    @Get(Paths.SEMANTIC_INDEX_PROFILES)
    List<Map<String, Object>> semanticIndexProfiles()

    @Post(Paths.SEMANTIC_INDEX_PROFILES)
    Map<String, Object> createSemanticIndexProfile(@Body Map<String, Object> request)

    @Delete(Paths.SEMANTIC_INDEX_PROFILE)
    Map<String, Object> deleteSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_ENABLE)
    Map<String, Object> enableSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_DISABLE)
    Map<String, Object> disableSemanticIndexProfile(@PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_LINK)
    Map<String, Object> linkSemanticIndexProfile(@PathVariable String indexName, @PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_PROFILE_UNLINK)
    Map<String, Object> unlinkSemanticIndexProfile(@PathVariable String indexName, @PathVariable String profileName)

    @Post(Paths.SEMANTIC_INDEX_EMBEDDING_MODEL_PULL)
    Map<String, Object> pullEmbeddingModel(@Body Map<String, Object> request)

    @Get(Paths.SEMANTIC_INDEXING_STATUS)
    Map<String, Object> semanticIndexingStatus()

    @Post(Paths.SEMANTIC_INDEXING_ENABLE)
    Map<String, Object> enableSemanticIndexing()

    @Post(Paths.SEMANTIC_INDEXING_DISABLE)
    Map<String, Object> disableSemanticIndexing()

    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_ENABLE)
    Map<String, Object> enableSemanticIndexingAutoReconcile()

    @Post(Paths.SEMANTIC_INDEXING_AUTO_RECONCILE_DISABLE)
    Map<String, Object> disableSemanticIndexingAutoReconcile()

    @Get(Paths.SEMANTIC_MODEL_INDEXES)
    List<Map<String, Object>> semanticModelIndexes()

    @Get(Paths.SEMANTIC_MODEL_INDEX_STATS)
    List<Map<String, Object>> semanticModelIndexStats(@PathVariable UUID modelId)

    @Post(Paths.SEMANTIC_MODEL_INDEXES)
    Map<String, Object> createSemanticModelIndex(@Body Map<String, Object> request)

    @Delete(Paths.SEMANTIC_MODEL_INDEX)
    Map<String, Object> deleteSemanticModelIndex(@PathVariable UUID modelId, @PathVariable String profileName)

    @Delete(Paths.SEMANTIC_MODEL_INDEX_DEFAULT_PROFILE)
    Map<String, Object> deleteDefaultSemanticModelIndex(@PathVariable UUID modelId)

    @Post(Paths.SEMANTIC_MODEL_INDEX_START)
    Map<String, Object> startSemanticModelIndex(@PathVariable UUID modelId, @PathVariable String profileName, @Body Map<String, Object> request)

    @Post(Paths.SEMANTIC_MODEL_INDEX_START_DEFAULT_PROFILE)
    Map<String, Object> startDefaultSemanticModelIndex(@PathVariable UUID modelId, @Body Map<String, Object> request)

    @Get(Paths.SEMANTIC_INDEX_JOBS)
    List<Map<String, Object>> semanticIndexJobs(@QueryValue(defaultValue = 'false') Boolean includeHistory)

    @Get(Paths.SEMANTIC_INDEX_JOB)
    Map<String, Object> semanticIndexJob(@PathVariable UUID jobId)

    @Post(Paths.SEMANTIC_INDEX_JOB_CANCEL)
    Map<String, Object> cancelSemanticIndexJob(@PathVariable UUID jobId)

    @Post(Paths.SEMANTIC_INDEX_JOB_RESUME)
    Map<String, Object> resumeSemanticIndexJob(@PathVariable UUID jobId)

    @Get(Paths.SEMANTIC_INDEX_JOB_EVENTS)
    @Produces('application/x-ndjson')
    Publisher<String> semanticIndexJobEvents(@PathVariable UUID jobId,
                                             @QueryValue(defaultValue = 'false') Boolean follow,
                                             @QueryValue(defaultValue = '0') Long after)

}
