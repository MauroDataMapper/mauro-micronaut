package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.web.ListResponse

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
