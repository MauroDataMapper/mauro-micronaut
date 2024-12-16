package uk.ac.ox.softeng.mauro.api.facet


import uk.ac.ox.softeng.mauro.domain.facet.Facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface FacetApi<I extends Facet> {

    @Get('/{id}')
    I show(UUID id)

    @Post
    I create(String domainType, UUID domainId, @Body @NonNull I facet)

    @Put('/{id}')
    I update(UUID id, @Body @NonNull I facet)

    @Delete('/{id}')
    HttpStatus delete(UUID id, @Body @Nullable I facet)

}