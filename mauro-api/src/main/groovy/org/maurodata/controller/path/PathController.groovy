package org.maurodata.controller.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.path.PathApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ItemController
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.Role
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
        AdministeredItem item = pathService.findResourceByPath(domainType, path)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with DomainType $domainType not found with path: $path")

        accessControlService.checkRole(Role.READER, item)
        pathService.updateDerivedProperties(item)
        item
    }

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH_FROM_RESOURCE)
    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String path) {
        AdministeredItem fromModel = findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, fromModel, "Model $domainType, $domainId not found")

        accessControlService.checkRole(Role.READER, fromModel)
        pathService.updateDerivedProperties(fromModel)

        //verify model path in the input path
        if (!path.contains(fromModel.path.pathString)) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Path $path does not belong to $domainType, $domainId")
        }
        Tuple2<String, String> itemDomainTypeAndPath = pathService.getItemDomainTypeAndPath(path)
        AdministeredItem administeredItem = pathService.findResourceByPath(itemDomainTypeAndPath.first, path)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, administeredItem, "Item with $itemDomainTypeAndPath.first  and label $itemDomainTypeAndPath.v2 not found")
        accessControlService.checkRole(Role.READER, administeredItem)
        pathService.updateDerivedProperties(administeredItem as I)
        administeredItem
    }

}
