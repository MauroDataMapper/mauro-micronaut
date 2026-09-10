package org.maurodata.plugin.chat.api.search

import org.maurodata.api.MauroApi
import org.maurodata.plugin.chat.api.Paths
import org.maurodata.plugin.chat.semantic.SetSemanticSearchRequest
import org.maurodata.plugin.chat.semantic.SetSemanticSearchResponse
import io.micronaut.http.annotation.*

@MauroApi
interface SetSemanticSearchApi {
    @Post(Paths.SEMANTIC_SET_SEARCH)
    SetSemanticSearchResponse search(@Body SetSemanticSearchRequest request)

    @Post(Paths.SEMANTIC_SET_CANDIDATES)
    SetSemanticSearchResponse candidates(@PathVariable String domainType, @PathVariable UUID setId, @Body SetSemanticSearchRequest request)

    @Post(Paths.SEMANTIC_SET_INDEX_REBUILD)
    Map<String, Object> rebuild(@PathVariable UUID modelId, @Body SetSemanticSearchRequest request)

    @Post(Paths.SEMANTIC_SET_INDEX_STATUS)
    Map<String, Object> status(@PathVariable UUID modelId, @Body SetSemanticSearchRequest request)
}
