package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/{domainType}/{domainId}/metadata')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface MetadataApi extends FacetApi<Metadata> {

    @Get
    ListResponse<Metadata> list(String domainType, UUID domainId)

}
