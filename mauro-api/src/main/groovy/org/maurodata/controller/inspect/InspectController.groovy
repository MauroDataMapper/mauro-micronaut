package org.maurodata.controller.inspect

import io.swagger.v3.oas.annotations.Operation
import org.maurodata.api.Paths
import org.maurodata.api.inspect.InspectApi
import org.maurodata.audit.Audit
import org.maurodata.service.inspect.InspectService

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class InspectController implements InspectApi {

    @Inject
    InspectService inspectService

    @Audit
    @Operation(operationId = 'inspectItem', summary = 'Inspect an item', description = 'Returns an inspect tree for an administered item. You must have read privileges on the item in question.')
    @Get(Paths.INSPECT_ITEM)
    Map<String, Object> inspect(String domainType, UUID id) {
        inspectService.inspect(domainType, id)
    }

    @Audit
    @Operation(operationId = 'inspectItemOverview', summary = 'Overview an item', description = 'Returns a compact textual overview for an administered item. You must have read privileges on the item in question.')
    @Get(Paths.INSPECT_ITEM_OVERVIEW)
    @Produces('text/plain;charset=UTF-8')
    String overview(String domainType, UUID id) {
        inspectService.overview(domainType, id)
    }
}
