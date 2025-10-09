package org.maurodata.service.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.controller.model.AvailableActions
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.domain.model.Path


@CompileStatic
@Slf4j
class PathService implements AdministeredItemReader {
    @Inject
    PathPrefixTypeLookup pathPrefixTypeLookup

    @Inject
    PathRepository pathRepository

    @Inject
    AccessControlService accessControlService

    AdministeredItem getResourceByPath(String domainType, String pathString) {
        Path path = new Path(pathString)
        AdministeredItem item = findResourceByPath(domainType, path)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with DomainType $domainType not found with path: $path")

        accessControlService.checkRole(Role.READER, item)
        updateDerivedProperties(item)
        item
    }


    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String pathString){
        AdministeredItem fromModel = findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, fromModel, "Model $domainType, $domainId not found")

        accessControlService.checkRole(Role.READER, fromModel)
        updateDerivedProperties(fromModel)

        //verify model path in the input path
        if (!pathString.contains(fromModel.path.pathString)) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Path $pathString does not belong to $domainType, $domainId")
        }
        Path path = new Path(pathString)
        Path.PathNode lastPathNode = path.lastPathNode()
        String itemDomainType = getDomainTypeFromPathPrefix(lastPathNode.prefix)
        AdministeredItem administeredItem = findResourceByPath(itemDomainType, path)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, administeredItem, "Item with ${lastPathNode.prefix}  and label ${lastPathNode.identifier} not found")
        accessControlService.checkRole(Role.READER, administeredItem)
        updateDerivedProperties(administeredItem)
        administeredItem
    }

    /**
     *
     * @param domainType et dataModels, folders, dataClasses
     * @param path -full path eg
     * http://localhost:8080/api/dataModels/path/dm%3AComplex%20Test%20DataModel%241.0.0 dm:Complex Test DataModel$1.0.0
     http://localhost:8080/api/folders/path/fo%3Asoluta%20eum%20architecto%7Cdm%3Amodi%20unde%20est%241.0.0%7Cdc%3Aest%20quasi%20vel
     http://localhost:8080/api/folders/path/fo%3Asoluta%20eum%20architecto%7Cdm%3Amodi%20unde%20est%24matrix

     From examples above,
     version could be branchname(defaults to main) or modelversion(nullable)

     * @return the admin item, given the full path including versioning
     */

    protected AdministeredItem findResourceByPath(String domainType, Path path) {
        String pathPrefix = getPathPrefixForDomainType(domainType)
        String domainPath = path.findLastPathNodeByPrefix(pathPrefix).identifier
        String versionString = path.modelIdentifier
        return findItemForPath(domainType, domainPath, versionString, path)
    }

    protected String getPathPrefixForDomainType(String domainType) {
        AdministeredItemCacheableRepository repo = repositoryService.administeredItemCacheableRepositories.find {
            it.handles(domainType)
        }
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, repo, "Cannot find repository for domain Type : $domainType")
        AdministeredItem domainClass = (AdministeredItem) repo.domainClass.getDeclaredConstructor().newInstance()
        return domainClass.getPathPrefix()
    }

    protected String getDomainTypeFromPathPrefix(String pathPrefix) {
        String domainType = pathPrefixTypeLookup.getDomainType(pathPrefix)
        if (!domainType) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Unknown path prefix $pathPrefix for modelItem ")
        }
        domainType
    }

    protected Item updateDerivedProperties(Item item) {
        pathRepository.readParentItems(item as AdministeredItem)
        (item as AdministeredItem).updatePath()
        (item as AdministeredItem).updateBreadcrumbs()
        AvailableActions.updateAvailableActions(item as AdministeredItem, accessControlService)
        item
    }

    protected AdministeredItem findItemForPath(String domainType, String domainPath, String versionString, Path path) {
        AdministeredItemCacheableRepository repository = getAdministeredItemRepository(domainType)
        List<AdministeredItem> items = repository.findAllByLabel(domainPath) as List<AdministeredItem>
        if (items.isEmpty()) {
            return null
        }
        AdministeredItem item
        if (items.size() == 1) {
            item = items[0] as AdministeredItem
        } else {
            if (!versionString) {
                log.warn("No version found in path: ${path.toString()}; returning 1st item")
                return items.first()
            }
            item = (items as List<AdministeredItem>).find {
                pathRepository.readParentItems(it)
                it.updatePath()
                it.path?.modelIdentifier == versionString
            }
        }
        item
    }
}
