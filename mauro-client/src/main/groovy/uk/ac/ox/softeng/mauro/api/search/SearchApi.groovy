package uk.ac.ox.softeng.mauro.api.search

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.domain.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.context.annotation.Parameter
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi
interface SearchApi {

    @Get(Paths.SEARCH_GET)
    ListResponse<SearchResultsDTO> searchGet(@Parameter SearchRequestDTO requestDTO)

    @Post(Paths.SEARCH_POST)
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO)

}
