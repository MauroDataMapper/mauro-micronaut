package org.maurodata.service.search

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Requires
import jakarta.inject.Singleton
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@CompileStatic
@Singleton
@Requires(missingBeans = SemanticSearchService)
class NoOpSemanticSearchService implements SemanticSearchService {

    static final String REASON = 'semantic search implementation is not installed'

    @Override
    ListResponse<SemanticSearchResultsDTO> executeSearch(SemanticSearchRequestDTO request,
                                                         BiFunction<String, UUID, AdministeredItem> itemLookup) {
        ListResponse.from([] as List<SemanticSearchResultsDTO>, request ?: new SemanticSearchRequestDTO())
    }

    @Override
    SemanticSearchAvailability availability(String indexName) {
        SemanticSearchAvailability.unavailable(REASON)
    }

    @Override
    SemanticSearchAvailability availability(String indexName, UUID mauroModelId) {
        SemanticSearchAvailability.unavailable(REASON)
    }

    @Override
    List<SearchResultsDTO> projectResults(List<SearchResultsDTO> sourceItems,
                                          List<String> targetDomainTypes) {
        sourceItems ?: [] as List<SearchResultsDTO>
    }
}
