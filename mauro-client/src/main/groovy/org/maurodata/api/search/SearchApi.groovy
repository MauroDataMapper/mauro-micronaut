package org.maurodata.api.search

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.search.dto.SearchRequestDTO
import org.maurodata.domain.search.dto.SearchResultsDTO
import org.maurodata.web.ListResponse

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
