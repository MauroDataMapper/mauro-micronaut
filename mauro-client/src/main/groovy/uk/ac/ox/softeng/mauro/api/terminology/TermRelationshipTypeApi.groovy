package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface TermRelationshipTypeApi extends AdministeredItemApi<TermRelationshipType, Terminology> {

    @Get(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType show(UUID terminologyId, UUID id)

    @Post(Paths.TERM_RELATIONSHIP_TYPE_LIST)
    TermRelationshipType create(UUID terminologyId, @Body @NonNull TermRelationshipType termRelationshipType)

    @Put(Paths.TERM_RELATIONSHIP_TYPE_ID)
    TermRelationshipType update(UUID terminologyId, UUID id, @Body @NonNull TermRelationshipType termRelationshipType)

    @Get(Paths.TERM_RELATIONSHIP_TYPE_LIST)
    ListResponse<TermRelationshipType> list(UUID terminologyId)

    @Delete(Paths.TERM_RELATIONSHIP_TYPE_ID)
    HttpStatus delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType)
}
