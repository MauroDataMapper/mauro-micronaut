package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.controller.model.ItemController

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.Facet
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository.SummaryMetadataCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.facet.SummaryMetadataReportRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class SummaryMetadataReportController extends ItemController<SummaryMetadataReport> implements SummaryMetadataReportApi {

    @Inject
    SummaryMetadataReportRepository summaryMetadataReportRepository

    @Inject
    SummaryMetadataCacheableRepository summaryMetadataCacheableRepository

    ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository
    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties()
    }

    SummaryMetadataReportController(ItemCacheableRepository.SummaryMetadataReportCacheableRepository summaryMetadataReportCacheableRepository) {
        super(summaryMetadataReportCacheableRepository)
        this.summaryMetadataReportCacheableRepository = summaryMetadataReportCacheableRepository
    }

    @Post(Paths.SUMMARY_METADATA_REPORTS_LIST)
    SummaryMetadataReport create(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @Body SummaryMetadataReport summaryMetadataReport) {
        super.cleanBody(summaryMetadataReport)
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(summaryMetadata))
        summaryMetadataReport.summaryMetadataId = summaryMetadata.id
        updateCreationProperties(summaryMetadataReport)
        summaryMetadataReportCacheableRepository.save(summaryMetadataReport)
    }

    @Get(Paths.SUMMARY_METADATA_REPORTS_LIST)
    ListResponse<SummaryMetadataReport> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId) {
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadata))
        List<SummaryMetadataReport> summaryMetadataReportList = summaryMetadataReportRepository.findAllBySummaryMetadataId(summaryMetadataId)
        ListResponse.from(summaryMetadataReportList)
    }

    @Get(Paths.SUMMARY_METADATA_REPORTS_ID)
    SummaryMetadataReport get(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                              @NonNull UUID id) {
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadata))
        summaryMetadataReportCacheableRepository.findById(id)
    }

    @Put(Paths.SUMMARY_METADATA_REPORTS_ID)
    SummaryMetadataReport update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                 @NonNull UUID id, @Body @NonNull SummaryMetadataReport summaryMetadataReport) {
        super.cleanBody(summaryMetadataReport)
        SummaryMetadataReport existing = summaryMetadataReportCacheableRepository.readById(id)
        throwNotFoundException(existing, id)
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(summaryMetadata))
        SummaryMetadataReport updated = updateEntity(existing, summaryMetadataReport, summaryMetadata)
        updated
    }

    @Delete(Paths.SUMMARY_METADATA_REPORTS_ID)
    @Transactional
    HttpStatus delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                      @NonNull UUID id) {
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(summaryMetadata))
        SummaryMetadataReport summaryMetadataReport = summaryMetadataReportCacheableRepository.readById(id)
        if (!summaryMetadataReport) {
            throwNotFoundException(summaryMetadataReport, id)
        }
        summaryMetadataReportCacheableRepository.delete(summaryMetadataReport, summaryMetadata)
        HttpStatus.NO_CONTENT
    }

    private SummaryMetadataReport updateEntity(SummaryMetadataReport existing, SummaryMetadataReport cleaned,
                                               SummaryMetadata summaryMetadata) {
        boolean hasChanged = updateProperties(existing, cleaned)
        if (hasChanged) {
            summaryMetadataReportCacheableRepository.update(existing, summaryMetadata)
        } else {
            existing
        }
    }

    private SummaryMetadata validateAndGet(String domainType, UUID domainId, UUID summaryMetadataId) {
        SummaryMetadata summaryMetadata = summaryMetadataCacheableRepository.readById(summaryMetadataId)
        if (!summaryMetadata) {
            throwNotFoundException(null, summaryMetadataId)
        }
        if (summaryMetadata.multiFacetAwareItemId != domainId || !summaryMetadataCacheableRepository.handles(domainType)) {
            throwNotFoundException(summaryMetadata, summaryMetadataId)
        }
        summaryMetadata
    }

    private static void throwNotFoundException(Item item, UUID id) {
        if (!item) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Item $id not found ")
        }
    }

    protected AdministeredItem readAdministeredItemForFacet(Facet facet) {
        readAdministeredItem(facet.multiFacetAwareItemDomainType, facet.multiFacetAwareItemId)
    }
}