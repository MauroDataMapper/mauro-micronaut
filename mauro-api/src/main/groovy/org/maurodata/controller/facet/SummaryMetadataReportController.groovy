package org.maurodata.controller.facet

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.api.Paths
import org.maurodata.api.facet.SummaryMetadataReportApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ItemController
import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.domain.facet.Facet
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository.SummaryMetadataCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.facet.SummaryMetadataReportRepository
import org.maurodata.web.ListResponse

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

    @Audit
    @Operation(operationId = 'createSummaryMetadataReport', summary = "Create a summary metadata report", description = "Creates a summary metadata report. You must have edit privileges on the item in question.")
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

    @Audit
    @Operation(operationId = 'listSummaryMetaReportPaged', summary = "List the summary metadata reports", description = "Returns the summary metadata reports. You must have read privileges on the item in question.")
    @Get(Paths.SUMMARY_METADATA_REPORTS_LIST_PAGED)
    ListResponse<SummaryMetadataReport> list(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                                             @Nullable PaginationParams params = new PaginationParams()) {
        
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadata))
        List<SummaryMetadataReport> summaryMetadataReportList = summaryMetadataReportRepository.findAllBySummaryMetadataId(summaryMetadataId)
        ListResponse.from(summaryMetadataReportList, params)
    }

    @Audit
    @Operation(operationId = 'showSummaryMetadataReport', summary = "Get a summary metadata report", description = "Returns a summary metadata report. You must have read privileges on the item in question.")
    @Get(Paths.SUMMARY_METADATA_REPORTS_ID)
    SummaryMetadataReport show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                               @NonNull UUID id) {
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.READER, readAdministeredItemForFacet(summaryMetadata))
        summaryMetadataReportCacheableRepository.findById(id)
    }

    @Audit
    @Operation(operationId = 'updateSummaryMetadataReport', summary = "Update a summary metadata report", description = "Updates a summary metadata report. You must have edit privileges on the item in question.")
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

    @Audit(deletedObjectDomainType = SummaryMetadataReport)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteSummaryMetadataReport', summary = "Delete a summary metadata report", description = "Deletes a summary metadata report. You must have edit privileges on the item in question.")
    @Delete(Paths.SUMMARY_METADATA_REPORTS_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID summaryMetadataId,
                        @NonNull UUID id) {
        SummaryMetadata summaryMetadata = validateAndGet(domainType, domainId, summaryMetadataId)
        accessControlService.checkRole(Role.EDITOR, readAdministeredItemForFacet(summaryMetadata))
        SummaryMetadataReport summaryMetadataReport = summaryMetadataReportCacheableRepository.readById(id)
        if (!summaryMetadataReport) {
            throwNotFoundException(summaryMetadataReport, id)
        }
        summaryMetadataReportCacheableRepository.delete(summaryMetadataReport, summaryMetadata)
        HttpResponse.status(HttpStatus.NO_CONTENT)
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