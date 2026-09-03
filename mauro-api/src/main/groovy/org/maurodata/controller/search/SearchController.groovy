package org.maurodata.controller.search

import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.search.SearchApi
import org.maurodata.audit.Audit

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.RequestBean
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.persistence.search.SearchIndexRefreshScheduler
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.security.AccessControlService
import org.maurodata.service.search.HybridSearchExecutionService
import org.maurodata.service.search.SearchExecutionService
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

    @Audit
    @Post(Paths.SEARCH_REBUILD_INDEXES)
    boolean rebuildIndexes() {
        accessControlService.checkAdministrator()
        log.warn("Rebuild index API endpoint called - ordinarily this should be for testing purposes only")
        searchIndexRefreshScheduler.refreshMaterializedViews()
        return true
    }

}
