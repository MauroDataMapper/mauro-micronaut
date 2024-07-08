package uk.ac.ox.softeng.mauro.controller.search

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemReader
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.security.AccessControlService
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@CompileStatic
@Controller('/')
@Secured(SecurityRule.IS_ANONYMOUS)
class SearchController implements AdministeredItemReader {

    @Inject
    SearchRepository searchRepository

    @Inject
    AccessControlService accessControlService

    @Get('/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO) {
        List<SearchResultsDTO> searchResults = searchRepository.search(requestDTO)
        List<SearchResultsDTO> searchResultsReadable = searchResults.findAll {SearchResultsDTO result ->
            AdministeredItem item = readAdministeredItem(result.domainType, result.id)
            accessControlService.canDoRole(Role.READER, item)
        }
        ListResponse.from(searchResultsReadable)
    }

    @Post('/search')
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO) {
        List<SearchResultsDTO> searchResults = searchRepository.search(requestDTO)
        List<SearchResultsDTO> searchResultsReadable = searchResults.findAll {SearchResultsDTO result ->
            AdministeredItem item = readAdministeredItem(result.domainType, result.id)
            accessControlService.canDoRole(Role.READER, item)
        }
        ListResponse.from(searchResultsReadable)
    }

}
