package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller('/{domainType}/{domainId}/edits')
@Secured(SecurityRule.IS_ANONYMOUS)
class EditController extends FacetController<Edit> {

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

    @Get
    ListResponse<Edit> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.edits ? []: administeredItem.edits)
    }
}
