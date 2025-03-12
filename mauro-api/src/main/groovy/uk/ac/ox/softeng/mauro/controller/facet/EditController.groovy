package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.EditApi
import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

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

    @Override
    @Get(Paths.EDIT_LIST)
    ListResponse<Edit> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.edits ? []: administeredItem.edits)
    }

    @Get(Paths.EDIT_ID)
    Edit show(@NonNull String domainType, @NonNull UUID domainId, @NonNull UUID id) {
        accessControlService.checkRole(Role.READER, readAdministeredItem(domainType, domainId))
        Edit validEdit = super.validateAndGet(domainType, domainId, id) as Edit
        validEdit
    }

    @Override
    @Delete(Paths.EDIT_ID)
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        super.delete(id)
    }
}
