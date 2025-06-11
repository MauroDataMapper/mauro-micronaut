package org.maurodata.controller.versionlinks

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_ANONYMOUS)
class VersionLinkController {

    //todo: implement actual
    @Get('/{catalogueItemDomainType}/{id}/versionLinks')
    List<Map> confirm(String catalogueItemDomainType, UUID id) {
        [
            [
                count: 0,
                items: []
            ]
        ] as List<Map>

    }

}
