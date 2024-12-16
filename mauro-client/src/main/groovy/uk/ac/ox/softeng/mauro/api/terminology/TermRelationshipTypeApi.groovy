package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.web.ListResponse

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
@Client('${micronaut.http.services.mauro.url}/terminologies/{terminologyId}/termRelationshipTypes')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface TermRelationshipTypeApi extends AdministeredItemApi<TermRelationshipType, Terminology> {

    @Get('/{id}')
    TermRelationshipType show(UUID terminologyId, UUID id)

    @Post
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType)

    @Put('/{id}')
    TermRelationshipType update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType)

    @Get
    ListResponse<TermRelationshipType> list(UUID terminologyId)

    @Delete('/{id}')
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType)
}
