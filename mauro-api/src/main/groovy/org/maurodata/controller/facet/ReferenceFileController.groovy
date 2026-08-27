package org.maurodata.controller.facet

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.api.Paths
import org.maurodata.api.facet.ReferenceFileApi
import org.maurodata.audit.Audit
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
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class ReferenceFileController extends FacetController<ReferenceFile> implements ReferenceFileApi {

    @Inject
    FacetCacheableRepository.ReferenceFileCacheableRepository referenceFileCacheableRepository

    ReferenceFileController(ItemCacheableRepository<ReferenceFile> facetRepository) {
        super(facetRepository)
    }

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }


    @Audit
    @Operation(operationId = 'listReferenceFilePaged', summary = "List the reference files", description = "Returns the reference files. You must have read privileges on the item in question.")
    @Get(Paths.REFERENCE_FILE_LIST_PAGED)
    ListResponse<ReferenceFile> list(String domainType, UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.referenceFiles ? [] : administeredItem.referenceFiles, params)
    }

    /**
     * getById
     * @param domainType
     * @param domainId
     * @param id
     * @return ReferenceFile
     */
    @Audit
    @Operation(summary = "Get a reference file", description = "Returns a reference file. You must have read privileges on the item in question.")
    @Get(Paths.REFERENCE_FILE_ID)
    byte[] showAndReturnFile(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        ReferenceFile validReferenceFile = super.validateAndGet(domainType, domainId, id) as ReferenceFile
        validReferenceFile.fileContent()
    }


    ReferenceFile show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        // Unused override of interface
        null
    }

    @Audit
    @Operation(operationId = 'updateReferenceFile', summary = "Update a reference file", description = "Updates a reference file.")
    @Put(Paths.REFERENCE_FILE_ID)
    ReferenceFile update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull ReferenceFile referenceFile) {
        super.update(id, referenceFile)
    }


    @Audit
    @Operation(operationId = 'createReferenceFile', summary = "Create a reference file", description = "Creates a reference file.")
    @Post(Paths.REFERENCE_FILE_LIST)
    ReferenceFile create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull ReferenceFile referenceFile) {
        referenceFile.setFileSize()
        super.create(domainType, domainId, referenceFile)
    }

    @Audit(deletedObjectDomainType = ReferenceFile)
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteReferenceFile', summary = "Delete a reference file", description = "Deletes a reference file. You must have edit privileges on the item in question.")
    @Delete(Paths.REFERENCE_FILE_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        super.validateAndGet(domainType, domainId, id)
        super.delete(id)
    }

}
