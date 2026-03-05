package org.maurodata.controller.search

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
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.search.SearchRepository
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.security.AccessControlService
import org.maurodata.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SearchController implements AdministeredItemReader, SearchApi {

    @Inject
    SearchRepository searchRepository

    @Inject
    AccessControlService accessControlService

    @Inject
    PathRepository pathRepository

    @Audit
    @Get(Paths.SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO) {
        List<SearchResultsDTO> searchResults = searchRepository.search(requestDTO)
        List<SearchResultsDTO> searchResultsReadable = searchResults.findAll {SearchResultsDTO result ->
            AdministeredItem item = readAdministeredItem(result.domainType, result.id)
            accessControlService.canDoRole(Role.READER, item)
        }
        ListResponse.from(searchResultsReadable)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO) {
        long startTime = System.currentTimeMillis()
        List<SearchResultsDTO> searchResults = searchRepository.search(requestDTO)
        System.err.println("Time taken 1: " + (System.currentTimeMillis() - startTime))
        //searchResults = searchResults.take(100)
        List<SearchResultsDTO> searchResultsReadable = searchResults.findAll {SearchResultsDTO result ->
            AdministeredItem item = readAdministeredItem(result.domainType, result.id)
            pathRepository.readParentItems(item)
            item.updateBreadcrumbs()
            result.breadcrumbs = item.breadcrumbs
            accessControlService.canDoRole(Role.READER, item)
        }
        System.err.println("Time taken 2: " + (System.currentTimeMillis() - startTime))
        ListResponse.from(searchResultsReadable, requestDTO)
    }

}
