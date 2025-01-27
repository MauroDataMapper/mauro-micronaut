package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.ReferenceFileApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ItemCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

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


    @Get(Paths.REFERENCE_FILE_LIST)
    ListResponse<ReferenceFile> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.referenceFiles ? []: administeredItem.referenceFiles)
    }
    /**
     * getById
     * @param domainType
     * @param domainId
     * @param id
     * @return ReferenceFile
     */
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

    @Put(Paths.REFERENCE_FILE_ID)
    ReferenceFile update(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id, @Body @NonNull ReferenceFile referenceFile) {
        super.update(id, referenceFile)
    }


    @Post(Paths.REFERENCE_FILE_LIST)
    ReferenceFile create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull ReferenceFile referenceFile) {
        referenceFile.setFileSize()
        super.create(domainType, domainId, referenceFile)
    }

    @Delete(Paths.REFERENCE_FILE_ID)
    @Transactional
    HttpResponse delete(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.EDITOR, readAdministeredItem(domainType, domainId))
        ReferenceFile referenceFileToDelete = super.validateAndGet(domainType, domainId, id) as ReferenceFile
        super.delete(id)
    }

}
