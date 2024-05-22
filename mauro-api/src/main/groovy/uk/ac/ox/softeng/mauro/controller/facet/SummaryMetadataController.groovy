package uk.ac.ox.softeng.mauro.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.SummaryMetadataReportRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/summaryMetadata')
@Secured(SecurityRule.IS_AUTHENTICATED)
class SummaryMetadataController extends FacetController<SummaryMetadata> {
    FacetCacheableRepository.SummaryMetadataCacheableRepository summaryMetadataRepository

    @Inject
    SummaryMetadataReportRepository summaryMetadataReportRepositoryUncached

    @Inject
    ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportRepository

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
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(administeredItem.summaryMetadata)
    }

    @Get('/{id}')
    SummaryMetadata show(UUID id) {
        SummaryMetadata summaryMetadata = super.show(id) as SummaryMetadata
        if (summaryMetadata) {
            AdministeredItem administeredItem = findAdministeredItem(summaryMetadata.multiFacetAwareItemDomainType,
                    summaryMetadata.multiFacetAwareItemId)
            accessControlService.checkRole(Role.READER, administeredItem)
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
    @Override
    HttpStatus delete(@NonNull UUID id, @Body @Nullable SummaryMetadata summaryMetadata) {
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadataRepository.readById(id)))
        deleteAnyAssociatedReports(id)
        super.delete(id, summaryMetadata)
    }

    private void deleteAnyAssociatedReports(UUID summaryMetadataId) {
        List<SummaryMetadataReport> savedSummaryMetadataReports = summaryMetadataReportRepositoryUncached.findAllBySummaryMetadataId(summaryMetadataId)
        if (savedSummaryMetadataReports) {
            summaryMetadataReportRepository.deleteAll(savedSummaryMetadataReports)
        }
    }
}