package uk.ac.ox.softeng.mauro.api.facet


import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/{domainType}/{domainId}/summaryMetadata/{summaryMetadataId}/summaryMetadataReports')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface SummaryMetadataReportApi {


    @Post
    SummaryMetadataReport create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @Body SummaryMetadataReport summaryMetadataReport)

    @Get
    ListResponse<SummaryMetadataReport> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId)

    @Get('/{id}')
    SummaryMetadataReport get(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                              @NonNull UUID id)

    @Put('/{id}')
    SummaryMetadataReport update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @NonNull UUID id, @Body @NonNull SummaryMetadataReport summaryMetadataReport)

    @Delete('/{id}')
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                      @NonNull UUID id)

}