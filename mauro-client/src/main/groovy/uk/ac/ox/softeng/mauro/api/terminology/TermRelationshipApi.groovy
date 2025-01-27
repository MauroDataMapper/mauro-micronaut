package uk.ac.ox.softeng.mauro.api.terminology

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface TermRelationshipApi extends AdministeredItemApi<TermRelationship, Terminology> {

    @Get(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship show(UUID terminologyId, UUID id)

    @Post(Paths.TERM_RELATIONSHIP_LIST)
    TermRelationship create(UUID terminologyId, @Body @NonNull TermRelationship termRelationship)

    @Put(Paths.TERM_RELATIONSHIP_ID)
    TermRelationship update(UUID terminologyId, UUID id, @Body @NonNull TermRelationship termRelationship)

    @Delete(Paths.TERM_RELATIONSHIP_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationship termRelationship)

    @Get(Paths.TERM_RELATIONSHIP_LIST)
    ListResponse<TermRelationship> list(UUID terminologyId)
}
