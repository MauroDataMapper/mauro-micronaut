package org.maurodata.controller.link

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import jakarta.inject.Inject
import org.maurodata.api.Paths
import org.maurodata.api.link.ItemLinkGraphApi
import org.maurodata.api.link.ItemLinkGraphDTO
import org.maurodata.api.link.ItemLinkKindDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.security.Role
import org.maurodata.security.AccessControlService
import org.maurodata.service.link.ItemLinkGraphService
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class ItemLinkGraphController implements ItemLinkGraphApi, AdministeredItemReader {
    @Inject ItemLinkGraphService itemLinkGraphService
    @Inject AccessControlService accessControlService

    @Override
    @Audit
    @Operation(operationId = 'listSupportedItemLinkKinds', summary = 'List supported item link kinds', description = 'Returns link kinds available for item graph traversal.')
    @Get(Paths.ITEM_LINK_KINDS)
    ListResponse<ItemLinkKindDTO> listSupportedLinkKinds() {
        ListResponse.from(itemLinkGraphService.supportedKinds())
    }

    @Override
    @Audit
    @Operation(operationId = 'traverseIncomingItemLinks', summary = 'Traverse incoming item links', description = 'Returns a breadth-first graph of incoming links and readable items for the selected link kinds.')
    @Get(Paths.ITEM_LINK_GRAPH)
    ItemLinkGraphDTO traverseIncomingLinks(String domainType, UUID domainId, @QueryValue('linkTypes') List<String> linkTypes) {
        AdministeredItem target = readAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, target)
        itemLinkGraphService.traverse(target, linkTypes)
    }
}
