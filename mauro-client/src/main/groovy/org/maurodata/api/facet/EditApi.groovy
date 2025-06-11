package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.Edit
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get

@MauroApi
interface EditApi extends FacetApi<Edit> {

    @Get(Paths.EDIT_LIST)
    ListResponse<Edit> list(@NonNull String domainType, @NonNull UUID domainId )

    @Get(Paths.EDIT_ID)
    Edit show(@NonNull String domainType, @NonNull UUID domainId, UUID id)

    @Delete(Paths.EDIT_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, UUID id)

}
