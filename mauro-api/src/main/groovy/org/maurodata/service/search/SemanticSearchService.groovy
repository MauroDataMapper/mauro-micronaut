package org.maurodata.service.search

import groovy.transform.CompileStatic
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.search.dto.SemanticSearchRequestDTO
import org.maurodata.domain.search.dto.SemanticSearchResultsDTO
import org.maurodata.web.ListResponse

import java.util.function.BiFunction

@CompileStatic
interface SemanticSearchService {

    ListResponse<SemanticSearchResultsDTO> executeSearch(SemanticSearchRequestDTO request,
                                                         BiFunction<String, UUID, AdministeredItem> itemLookup)

    SemanticSearchAvailability availability(String indexName)
}
