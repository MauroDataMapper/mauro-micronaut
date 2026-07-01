package org.maurodata.domain.search.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable

@Introspected
@CompileStatic
class SemanticSearchRequestDTO extends SearchRequestDTO {

    @Nullable
    String query

    @Nullable
    String corpus = 'catalogue-items'

    @Nullable
    String indexName = 'catalogue-items-default'

    @Nullable
    List<String> embeddingProfiles = []

    @Nullable
    Integer topN = 50

    @Nullable
    Integer topM = 10

    @Nullable
    Boolean includeChunks = true

    @Nullable
    Boolean rebuildIfEmpty = false

    @Nullable
    Boolean deepSearch = false

    @Nullable
    UUID likeItemId

    @Nullable
    String likeItemDomainType
}
