package org.maurodata.controller.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.api.Paths
import org.maurodata.api.path.PathApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.persistence.model.ItemRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.service.path.PathService

@Slf4j
@Singleton
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@CompileStatic
class PathController<I extends Item> extends ItemController<I> implements PathApi<I> {

    @Inject
    PathService pathService

    PathController(PathRepository pathRepository) {
        super(pathRepository as ItemRepository<I>)
    }

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH)
    AdministeredItem getResourceByPath(String domainType, String path) {
        pathService.getResourceByPath(domainType, path)
    }

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH_FROM_RESOURCE)
    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String path) {
        pathService.getResourceByPathFromResource(domainType, domainId, path)

    }

}
