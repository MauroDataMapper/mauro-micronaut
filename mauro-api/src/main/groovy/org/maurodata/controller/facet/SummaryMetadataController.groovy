package org.maurodata.controller.facet

import org.maurodata.api.Paths
import org.maurodata.api.facet.SummaryMetadataApi
import org.maurodata.audit.Audit
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.facet.SummaryMetadataReportRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SummaryMetadataController extends FacetController<SummaryMetadata> implements SummaryMetadataApi {
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

    @Audit
    @Get(Paths.SUMMARY_METADATA_LIST_PAGED)
    ListResponse<SummaryMetadata> list(String domainType, UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)

        ListResponse<SummaryMetadata>.from(administeredItem.summaryMetadata, params)
    }

    @Audit
    @Get(Paths.SUMMARY_METADATA_ID)
    SummaryMetadata show(@NonNull String domainType, @NonNull UUID domainId, UUID id) {
        SummaryMetadata summaryMetadata = super.show(domainType, domainId, id) as SummaryMetadata
        if (summaryMetadata) {
            AdministeredItem administeredItem = findAdministeredItem(summaryMetadata.multiFacetAwareItemDomainType,
                    summaryMetadata.multiFacetAwareItemId)
            accessControlService.checkRole(Role.READER, administeredItem)
            List<SummaryMetadata> summaryMetadataList = administeredItem.summaryMetadata
            SummaryMetadata summaryMetadataWithReports = summaryMetadataList.find { it -> it.id == id }
            summaryMetadataWithReports
        }
    }

    @Audit
    @Post(Paths.SUMMARY_METADATA_LIST)
    SummaryMetadata create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SummaryMetadata summaryMetadata) {
        super.create(domainType, domainId, summaryMetadata)
    }

    @Audit
    @Override
    @Put(Paths.SUMMARY_METADATA_ID)
    SummaryMetadata update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull SummaryMetadata summaryMetadata) {
        update(id, summaryMetadata)
    }

    @Audit(deletedObjectDomainType = SummaryMetadata)
    @Delete(Paths.SUMMARY_METADATA_ID)
    @Transactional
    @Override
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadataRepository.readById(id)))
        deleteAnyAssociatedReports(id)
        super.delete(id)
    }

    private void deleteAnyAssociatedReports(UUID summaryMetadataId) {
        List<SummaryMetadataReport> savedSummaryMetadataReports = summaryMetadataReportRepositoryUncached.findAllBySummaryMetadataId(summaryMetadataId)
        if (savedSummaryMetadataReports) {
            summaryMetadataReportRepository.deleteAll(savedSummaryMetadataReports)
        }
    }
}