package org.maurodata.controller.facet

import org.maurodata.api.Paths
import org.maurodata.api.facet.EditApi
import org.maurodata.audit.Audit
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class EditController extends FacetController<Edit> implements EditApi {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.EditCacheableRepository editRepository

    EditController(FacetCacheableRepository.EditCacheableRepository editRepository) {
        super(editRepository)
        this.editRepository = editRepository
    }

    @Audit
    @Override
    @Get(Paths.EDIT_LIST)
    ListResponse<Edit> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.edits ? []: administeredItem.edits)
    }

    @Audit
    @Get(Paths.EDIT_ID)
    Edit show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Edit validEdit = super.validateAndGet(domainType, domainId, id) as Edit
        validEdit
    }

    @Override
    @Audit(deletedObjectDomainType = Edit)
    @Delete(Paths.EDIT_ID)
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        super.delete(id)
    }
}
