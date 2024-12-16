package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
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
@Client('${micronaut.http.services.mauro.url}/{domainType}/{domainId}/summaryMetadata')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface SummaryMetadataApi extends FacetApi<SummaryMetadata> {

    @Get
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId)

    @Get('/{id}')
    SummaryMetadata show(UUID id)

    @Post
    SummaryMetadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SummaryMetadata summaryMetadata)

    @Put('/{id}')
    SummaryMetadata update(@NonNull UUID id, @Body @NonNull SummaryMetadata summaryMetadata)

    @Delete('/{id}')
    @Override
    HttpStatus delete(@NonNull UUID id, @Body @Nullable SummaryMetadata summaryMetadata)

}