package org.maurodata.api.facet

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface SummaryMetadataReportApi {


    @Post(Paths.SUMMARY_METADATA_REPORTS_LIST)
    SummaryMetadataReport create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @Body SummaryMetadataReport summaryMetadataReport)

    @Get(Paths.SUMMARY_METADATA_REPORTS_LIST)
    ListResponse<SummaryMetadataReport> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId)

    @Get(Paths.SUMMARY_METADATA_REPORTS_ID)
    SummaryMetadataReport show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                              @NonNull UUID id)

    @Put(Paths.SUMMARY_METADATA_REPORTS_ID)
    SummaryMetadataReport update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @NonNull UUID id, @Body @NonNull SummaryMetadataReport summaryMetadataReport)

    @Delete(Paths.SUMMARY_METADATA_REPORTS_ID)
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                        @NonNull UUID id)

}