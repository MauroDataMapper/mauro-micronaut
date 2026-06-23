package org.maurodata.api.search

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.web.ListResponse

import io.micronaut.context.annotation.Parameter
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.http.annotation.Post

@MauroApi
interface SearchApi {

    @Get(Paths.SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(@Parameter SearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO)

    @Get(Paths.SEARCH_KEYWORD_GET)
    ListResponse<SearchResultsDTO> keywordSearchGet(@Parameter SearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_KEYWORD_POST)
    ListResponse<SearchResultsDTO> keywordSearchPost(@Body SearchRequestDTO requestDTO)

    @Get(Paths.SEARCH_SEMANTIC_GET)
    ListResponse<SemanticSearchResultsDTO> semanticSearchGet(@Parameter SemanticSearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_SEMANTIC_POST)
    ListResponse<SemanticSearchResultsDTO> semanticSearchPost(@Body SemanticSearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_REBUILD_INDEXES)
    boolean rebuildIndexes()

    @Post(Paths.SEARCH_REBUILD_SEMANTIC_INDEXES)
    Map<String, Object> rebuildSemanticIndexes(@Body Map<String, Object> request)

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

}
