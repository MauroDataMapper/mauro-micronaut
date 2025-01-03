package uk.ac.ox.softeng.mauro.api.facet

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface SummaryMetadataApi extends FacetApi<SummaryMetadata> {

    @Get
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId)

    @Get(Paths.SUMMARY_METADATA_ID)
    SummaryMetadata show(@NonNull String domainType, @NonNull UUID domainId, UUID id)

    @Post(Paths.SUMMARY_METADATA_LIST)
    SummaryMetadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SummaryMetadata summaryMetadata)

    @Put(Paths.SUMMARY_METADATA_ID)
    SummaryMetadata update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull SummaryMetadata summaryMetadata)

    @Delete(Paths.SUMMARY_METADATA_ID)
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id)

}