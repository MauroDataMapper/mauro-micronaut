package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.api.facet.MetadataApi

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/{domainType}/{domainId}/metadata')
@Secured(SecurityRule.IS_ANONYMOUS)
class MetadataController extends FacetController<Metadata> implements MetadataApi {

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.MetadataCacheableRepository metadataRepository

    MetadataController(FacetCacheableRepository.MetadataCacheableRepository metadataRepository) {
        super(metadataRepository)
        this.metadataRepository = metadataRepository
    }

    @Get
    ListResponse<Metadata> list(String domainType, UUID domainId) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, administeredItem)
        ListResponse.from(!administeredItem.metadata ? []: administeredItem.metadata)
    }
}
