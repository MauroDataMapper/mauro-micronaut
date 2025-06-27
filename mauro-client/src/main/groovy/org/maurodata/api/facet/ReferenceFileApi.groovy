package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.ReferenceFile
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
interface ReferenceFileApi extends FacetApi<ReferenceFile> {

    @Get(Paths.REFERENCE_FILE_LIST)
    ListResponse<ReferenceFile> list(String domainType, UUID domainId)

    @Get(Paths.REFERENCE_FILE_LIST_PAGED)
    ListResponse<ReferenceFile> list(String domainType, UUID domainId, @Nullable PaginationParams params)

    @Get(Paths.REFERENCE_FILE_ID)
    byte[] showAndReturnFile(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)


    /**
     * getById
     * @param domainType
     * @param domainId
     * @param id
     * @return ReferenceFile
     */
    @Get(Paths.REFERENCE_FILE_ID)
    ReferenceFile show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Put(Paths.REFERENCE_FILE_ID)
    ReferenceFile update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull ReferenceFile referenceFile)


    @Post(Paths.REFERENCE_FILE_LIST)
    ReferenceFile create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull ReferenceFile referenceFile)

    @Delete(Paths.REFERENCE_FILE_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

}
