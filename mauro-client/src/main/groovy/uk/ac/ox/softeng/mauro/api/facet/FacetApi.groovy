package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.domain.facet.Facet

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put


@MauroApi
interface FacetApi<I extends Facet> {

    @Get('/{id}')
    I show(String domainType, UUID domainId, UUID id)

    @Post
    I create(String domainType, UUID domainId, @Body @NonNull I facet)

/*  Don't enforce this on facets
    @Put('/{id}')
    I update(String domainType, UUID domainId, UUID id, @Body @NonNull I facet)
 */

    @Delete('/{id}')
    HttpStatus delete(String domainType, UUID domainId, UUID id)

}