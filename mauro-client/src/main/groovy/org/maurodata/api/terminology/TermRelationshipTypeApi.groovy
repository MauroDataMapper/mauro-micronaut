package org.maurodata.api.terminology

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
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

    @Get(Paths.TERM_RELATIONSHIP_TYPE_LIST_PAGED)
    ListResponse<TermRelationshipType> list(UUID terminologyId, @Nullable PaginationParams params)

    @Delete(Paths.TERM_RELATIONSHIP_TYPE_ID)
    HttpResponse delete(UUID terminologyId, UUID id, @Body @Nullable TermRelationshipType termRelationshipType)
}
