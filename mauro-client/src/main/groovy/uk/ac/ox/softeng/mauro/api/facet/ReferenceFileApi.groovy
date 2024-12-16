package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
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
@Client('${micronaut.http.services.mauro.url}')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface ReferenceFileApi extends FacetApi<ReferenceFile> {

    @Get('/{domainType}/{domainId}/referenceFiles')
    ListResponse<ReferenceFile> list(String domainType, UUID domainId)

    /**
     * getById
     * @param domainType
     * @param domainId
     * @param id
     * @return ReferenceFile
     */
    @Get('/{domainType}/{domainId}/referenceFiles/{id}')
    byte[] show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

    @Post('/{domainType}/{domainId}/referenceFiles')
    ReferenceFile create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull ReferenceFile referenceFile)

    @Put('/{domainType}/{domainId}/referenceFiles/{id}')
    ReferenceFile update(UUID domainId, @NonNull UUID id, @Body @NonNull ReferenceFile referenceFile)

    @Delete('/{domainType}/{domainId}/referenceFiles/{id}')
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id,
                      @Body @Nullable ReferenceFile referenceFile)

}
