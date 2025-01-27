package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface MetadataApi extends FacetApi<Metadata> {

    @Get(Paths.METADATA_LIST)
    ListResponse<Metadata> list(@NonNull String domainType, @NonNull UUID domainId )

    @Get(Paths.METADATA_ID)
    Metadata show(@NonNull String domainType, @NonNull UUID domainId, UUID id)

    @Put(Paths.METADATA_ID)
    Metadata update(@NonNull String domainType, @NonNull UUID domainId, UUID id, @Body @NonNull Metadata metadata)

    @Post(Paths.METADATA_LIST)
    Metadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull Metadata metadata)

    @Delete(Paths.METADATA_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, UUID id)

}
