package uk.ac.ox.softeng.mauro.controller.search


import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.annotation.Body
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRepository
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@CompileStatic
@Controller('/')
class SearchController {

    @Inject
    SearchRepository searchRepository

    @Get('/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO) {
        ListResponse.from(searchRepository.search(requestDTO))
    }

    @Post('/search')
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO) {
        ListResponse.from(searchRepository.search(requestDTO))
    }

}
