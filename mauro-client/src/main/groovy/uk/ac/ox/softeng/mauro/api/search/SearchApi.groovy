package uk.ac.ox.softeng.mauro.api.search

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchRequestDTO
import uk.ac.ox.softeng.mauro.persistence.search.dto.SearchResultsDTO
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.RequestBean
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface SearchApi {

    @Get('/search{?requestDTO}')
    ListResponse<SearchResultsDTO> searchGet(@RequestBean SearchRequestDTO requestDTO)

    @Post('/search')
    ListResponse<SearchResultsDTO> searchPost(@Body SearchRequestDTO requestDTO)

}
