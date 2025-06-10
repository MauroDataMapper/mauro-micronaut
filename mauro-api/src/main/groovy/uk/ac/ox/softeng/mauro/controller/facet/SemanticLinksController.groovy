package uk.ac.ox.softeng.mauro.controller.facet

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.facet.SemanticLinkDTO
import uk.ac.ox.softeng.mauro.api.facet.SemanticLinksApi
import uk.ac.ox.softeng.mauro.domain.facet.SemanticLink
import uk.ac.ox.softeng.mauro.domain.facet.SemanticLinkType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.FacetCacheableRepository
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_ANONYMOUS)
class SemanticLinksController extends FacetController<SemanticLink> implements SemanticLinksApi {

    //todo: implement actual

    /**
     * Properties disallowed in a simple update request.
     */
    List<String> getDisallowedProperties() {
        super.getDisallowedProperties() + ['multiFacetAwareItemDomainType', 'multiFacetAwareItemId']
    }

    FacetCacheableRepository.SemanticLinkCacheableRepository semanticLinkRepository

    SemanticLinksController(FacetCacheableRepository.SemanticLinkCacheableRepository semanticLinkRepository) {
        super(semanticLinkRepository)
        this.semanticLinkRepository = semanticLinkRepository
    }

    @Get(Paths.SEMANTIC_LINKS_LIST_PAGED)
    ListResponse<SemanticLinkDTO> list(@NonNull String domainType, @NonNull UUID domainId, @Nullable PaginationParams params = new PaginationParams()) {
        AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, administeredItem, "$domainType $domainId not found")
        accessControlService.checkRole(Role.READER, administeredItem)

        List<SemanticLink> semanticLinks = !administeredItem.semanticLinks ? [] : administeredItem.semanticLinks
        List<SemanticLinkDTO> semanticLinkDTOList = []

        semanticLinks.forEach {SemanticLink semanticLink ->

            final String targetDomainType = semanticLink.targetMultiFacetAwareItemDomainType
            final UUID targetDomainId = semanticLink.targetMultiFacetAwareItemId

            // RHS
            final AdministeredItem targetAdministeredItem = findAdministeredItem(targetDomainType, targetDomainId)

            if (targetAdministeredItem != null) {
                final SemanticLinkDTO semanticLinkDTO = new SemanticLinkDTO().tap {
                    id = semanticLink.id
                    linkType = semanticLink.linkType.label
                    unconfirmed = semanticLink.unconfirmed
                    sourceMultiFacetAwareItem = administeredItem
                    targetMultiFacetAwareItem = targetAdministeredItem
                }

                semanticLinkDTOList << semanticLinkDTO
            }
        }

        return ListResponse.from(semanticLinkDTOList, params)
    }

    @Override
    HttpResponse delete(String domainType, UUID domainId, UUID id) {
        return null
    }

    @Override
    SemanticLinkDTO create(@NonNull String domainType, @NonNull UUID domainId, @Body @NonNull SemanticLinkDTO semanticLink) {

        // LHS
        final AdministeredItem administeredItem = findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, administeredItem, "$domainType $domainId not found")
        accessControlService.checkRole(Role.READER, administeredItem)

        final String targetDomainType = semanticLink.targetMultiFacetAwareItemDomainType
        final UUID targetDomainId = semanticLink.targetMultiFacetAwareItemId
        // RHS
        final AdministeredItem targetAdministeredItem = findAdministeredItem(targetDomainType, targetDomainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, targetAdministeredItem, "$targetDomainType $targetDomainId not found")
        accessControlService.checkRole(Role.READER, targetAdministeredItem)

        // Create a SemanticLink object from this SemanticLinkDTO, and create it

        final SemanticLink semanticLinkToCreate = new SemanticLink().tap {
            linkType = SemanticLinkType.semanticLinkTypeForLabel(semanticLink.linkType)
            targetMultiFacetAwareItemId = semanticLink.targetMultiFacetAwareItemId
            targetMultiFacetAwareItemDomainType = semanticLink.targetMultiFacetAwareItemDomainType
            unconfirmed = false
        }

        final SemanticLink createdSemanticLink = super.create(domainType, domainId, semanticLinkToCreate) as SemanticLink

        // return a new SemanticLinkDTO

        final SemanticLinkDTO createdSemanticLinkDTO = new SemanticLinkDTO().tap {

            id = createdSemanticLink.id
            linkType = createdSemanticLink.linkType.label
            unconfirmed = createdSemanticLink.unconfirmed
            sourceMultiFacetAwareItem = administeredItem
            targetMultiFacetAwareItem = targetAdministeredItem
        }

        return createdSemanticLinkDTO
    }
}
