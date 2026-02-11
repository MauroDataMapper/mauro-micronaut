package org.maurodata.service.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.ErrorHandler
import org.maurodata.controller.model.AdministeredItemReader
import org.maurodata.controller.model.AvailableActions
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.Path
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService

import static org.maurodata.util.PathStringUtils.getCOLON
import static org.maurodata.util.PathStringUtils.getDISCARD_AFTER_VERSION
import static org.maurodata.util.PathStringUtils.getItemSubPath
import static org.maurodata.util.PathStringUtils.getREMOVE_VERSION_DELIM
import static org.maurodata.util.PathStringUtils.getVersionFromPath
import static org.maurodata.util.PathStringUtils.lastSubPath
import static org.maurodata.util.PathStringUtils.splitBy

@CompileStatic
@Slf4j
@Singleton
class PathService implements AdministeredItemReader {
    @Inject
    PathPrefixTypeLookup pathPrefixTypeLookup

    @Inject
    PathRepository pathRepository

    @Inject
    AccessControlService accessControlService

    AdministeredItem getResourceByPath(String domainType, String path) {
        AdministeredItem item = findResourceByPath(domainType, path)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, item, "Item with DomainType $domainType not found with path: $path")

        accessControlService.checkRole(Role.READER, item)
        updateDerivedProperties(item)
        item
    }


    AdministeredItem getResourceByPathFromResource(String domainType, UUID domainId, String path){
        AdministeredItem fromItem = findAdministeredItem(domainType, domainId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, fromItem, "Model $domainType, $domainId not found")

        accessControlService.checkRole(Role.READER, fromItem)
        pathRepository.readParentItems(fromItem)

        Model owningModel = fromItem.getOwner()
        if (!owningModel) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item (${domainType}, ${domainId}) does not have an owning model")
        }
        AdministeredItem administeredItem = findItemByPath(owningModel, new Path(path).nodes)

        accessControlService.checkRole(Role.READER, administeredItem)
        return administeredItem

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

    protected AdministeredItem findResourceByPath(String domainType, String path) {
        String pathPrefix = getPathPrefixForDomainType(domainType)
        String domainPath = getItemSubPath(pathPrefix, path)
        String versionString = getVersionFromPath(path)
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

    protected AdministeredItem findItemForPath(String domainType, String domainPath, String versionString, String fullPath) {
        AdministeredItemCacheableRepository repository = getAdministeredItemRepository(domainType)
        List<AdministeredItem> items = repository.findAllByLabel(domainPath)
        if (items.isEmpty()) {
            null
        }
        AdministeredItem item
        if (items.size() == 1) {
            item = items[0] as AdministeredItem
        } else {
            if (!versionString) {
                log.warn("No version found in  fullpath: $fullPath; returning 1st item")
                items.first()
            }
            item = (items as List<AdministeredItem>).find {
                pathRepository.readParentItems(it)
                it.updatePath()
                it.path?.pathString?.contains(versionString)
            }
        }
        item
    }



    AdministeredItem findItemByPath(AdministeredItem parent, List<Path.PathNode> pathNodes) {
        if(!pathNodes || pathNodes.size() == 0) {
            return parent
        }
        Path.PathNode firstPathNode = pathNodes.remove(0)
        String domainType = getDomainTypeFromPathPrefix(firstPathNode.prefix)
        AdministeredItemCacheableRepository repository = getAdministeredItemRepository(domainType)
        List<AdministeredItem> items = repository.findAllByLabel(firstPathNode.identifier) as List<AdministeredItem>
        AdministeredItem nextItemInPath = null
        if(firstPathNode.modelIdentifier) {
            // We've got a new root node - find it and then start from there
            nextItemInPath = items.find {it ->
                Model model = it as Model
                model.modelVersionTag == firstPathNode.modelIdentifier ||
                model.branchName == firstPathNode.modelIdentifier ||
                model.modelVersion.toString() == firstPathNode.modelIdentifier
            }
        } else {
            nextItemInPath = items.find {it.parent && it.parent.id == parent.id }
        }
        if(!nextItemInPath) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Unknown path component: ${firstPathNode.toString()}")
        }
        return findItemByPath(nextItemInPath, pathNodes)
    }
}
