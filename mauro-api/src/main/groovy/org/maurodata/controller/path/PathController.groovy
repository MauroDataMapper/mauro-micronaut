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
        String pathPrefix = pathService.getPathPrefixForDomainType(domainType)
        String domainPath = pathService.getPathPrefixSubPath(pathPrefix, path)
        AdministeredItem item = findAdministeredItem(domainType, domainPath) as AdministeredItem
        accessControlService.checkRole(Role.READER, item)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with DomainType $domainType, label: $domainPath not found ")
        pathService.updateDerivedProperties(item)
        item
    }

    @Audit
    @Override
    @Get(Paths.RESOURCE_BY_PATH_FROM_RESOURCE)
    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String path) {
        AdministeredItem fromModel = findAdministeredItem(domainType, domainId)
        accessControlService.checkRole(Role.READER, fromModel)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, fromModel, "Model $domainType, $domainId not found")
        pathService.updateDerivedProperties(fromModel)

        //verify model path contained in item full path
        String modelPath = fromModel.path.pathString.split(PathService.VERTICAL_BAR_ESCAPE).reverse().first()
        //verify model in the input path
        if (!path.contains(modelPath)) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Path $path does not belong to $domainType, $domainId")
        }
        String itemPath = pathService.getItemPath(path)
        //extract the item domain type from given input path
        String[] itemParts = pathService.splitBy(itemPath, PathService.COLON)
        String itemDomainType = pathService.getDomainTypeFromPathPrefix(itemParts[0])

        AdministeredItem item = findAdministeredItem(itemDomainType,itemParts[1].strip() ) as AdministeredItem
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with $itemDomainType and label ${itemParts[1]}  not found")
        accessControlService.checkRole(Role.READER, item)

        pathService.updateDerivedProperties(item as I)
        item
    }
}
