package org.maurodata.controller.facet

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.facet.VersionLinksApi
import org.maurodata.api.model.ModelRefDTO
import org.maurodata.api.model.VersionLinkDTO
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.FacetCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class VersionLinksController extends FacetController<VersionLink> implements VersionLinksApi {

    @Inject
    FacetCacheableRepository.VersionLinkCacheableRepository versionLinkCacheableRepository

    VersionLinksController(ItemCacheableRepository<VersionLink> facetRepository) {
        super(facetRepository)
    }

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    @Override
    @Get(Paths.VERSION_LINKS_LIST_PAGED)
    ListResponse<VersionLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {

        Model administeredItem = (Model) findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, administeredItem, "$domainType $domainId not found")
        accessControlService.checkRole(Role.READER, administeredItem)

        final List<VersionLinkDTO> versionList = new ArrayList<>()

        for (VersionLink versionLink : administeredItem.versionLinks) {
            versionList.add(constructVersionLinkDTO((Model) administeredItem, versionLink))
        }

        return ListResponse.from(versionList)
    }

    protected VersionLinkDTO constructVersionLinkDTO(final Model sourceModel, final VersionLink versionLink) {
        // Look up target model

        final Model targetModel = (Model) findAdministeredItem(versionLink.targetModelDomainType, versionLink.targetModelId)
        if (targetModel == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }

        ModelRefDTO sourceModelDto = new ModelRefDTO(sourceModel)
        ModelRefDTO targetModelDto = new ModelRefDTO(targetModel)

        final VersionLinkDTO versionLinkDTO = new VersionLinkDTO(id: versionLink.id, linkType: versionLink.versionLinkType, sourceModel: sourceModelDto,
                                                                 targetModel: targetModelDto)
        versionLinkDTO
    }

    @Override
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        return null
    }


}
