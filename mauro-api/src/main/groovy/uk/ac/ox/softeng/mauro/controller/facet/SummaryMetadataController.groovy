package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.SummaryMetadataReportRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/summaryMetadata')
class SummaryMetadataController extends FacetController<SummaryMetadata> {
    FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataRepository

    @Inject
    SummaryMetadataReportRepository summaryMetadataReportRepository

    @Inject
    ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    SummaryMetadataController(FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataRepository) {
        super(summaryMetadataRepository)
        this.summaryMetadataRepository = summaryMetadataRepository
    }

    @Get
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ListResponse.from(!administeredItem.summaryMetadata ? [] : administeredItem.summaryMetadata)
    }

    @Get('/{id}')
    SummaryMetadata show(UUID id) {
        SummaryMetadata summaryMetadata = super.show(id) as SummaryMetadata
        if (summaryMetadata) {
            AdministeredItem administeredItem = findAdministeredItem(summaryMetadata.multiFacetAwareItemDomainType,
                    summaryMetadata.multiFacetAwareItemId)
            List<SummaryMetadata> summaryMetadataList = administeredItem.summaryMetadata
            SummaryMetadata summaryMetadataWithReports = summaryMetadataList.find { it -> it.id == id }
            summaryMetadataWithReports
        }
    }

    @Post
    SummaryMetadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SummaryMetadata summaryMetadata) {
        super.create(domainType, domainId, summaryMetadata)
    }

    @Put('/{id}')
    SummaryMetadata update(@NonNull UUID id, @Body @NonNull SummaryMetadata summaryMetadata) {
        super.update(id, summaryMetadata)
    }

    @Delete('/{id}')
    @Transactional
    HttpStatus delete(@NonNull UUID id, @Body @Nullable SummaryMetadata summaryMetadata) {
        deleteAnyAssociatedReports(id)
        super.delete(id, summaryMetadata)
    }

    private void deleteAnyAssociatedReports(UUID summaryMetadataId) {
        List<SummaryMetadataReport> savedSummaryMetadataReports = summaryMetadataReportRepository.findAllBySummaryMetadataId(summaryMetadataId)
        if (savedSummaryMetadataReports) {
            summaryMetadataReportCacheableRepository.deleteAll(savedSummaryMetadataReports)
        }
    }
}