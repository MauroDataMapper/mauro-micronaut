package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface ReferenceFileApi extends FacetApi<ReferenceFile> {

    @Get(Paths.REFERENCE_FILE_LIST)
    ListResponse<ReferenceFile> list(String domainType, UUID domainId)

    @Get(Paths.REFERENCE_FILE_ID)
    byte[] showAndReturnFile(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)


        /**
     * getById
     * @param domainType
     * @param domainId
     * @param id
     * @return ReferenceFile
     */
    ReferenceFile show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Put(Paths.REFERENCE_FILE_ID)
    ReferenceFile update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull ReferenceFile referenceFile)


    @Post(Paths.REFERENCE_FILE_LIST)
    ReferenceFile create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull ReferenceFile referenceFile)

    @Delete(Paths.REFERENCE_FILE_ID)
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

}
