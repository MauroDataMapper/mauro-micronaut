package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Get

@MauroApi
interface EditApi {

    @Get(Paths.EDIT_LIST)
    ListResponse list(@NonNull String domainType, @NonNull UUID domainId )

}