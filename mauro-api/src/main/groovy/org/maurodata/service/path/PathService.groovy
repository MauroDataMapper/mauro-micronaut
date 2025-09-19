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
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService
import org.maurodata.utils.PathStringUtils

@CompileStatic
@Slf4j
class PathService implements AdministeredItemReader {
    @Inject
    PathPrefixTypeLookup pathPrefixTypeLookup

    @Inject
    PathRepository pathRepository

    @Inject
    AccessControlService accessControlService

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

    AdministeredItem findResourceByPath(String domainType, String path) {
        String pathPrefix = getPathPrefixForDomainType(domainType)
        String domainPath = PathStringUtils.getItemSubPath(pathPrefix, path)
        String versionString = PathStringUtils.getVersionFromPath(path)
        return findItemForPath(domainType, domainPath, versionString, path)
    }


    /**
     *
     * @param path fullPath
     * @return  the last path domainType and label/subPath
     */
    Tuple2<String,String> getItemDomainTypeAndPath(String path) {
        String itemPath = PathStringUtils.lastSubPath(path)
        //extract the item domain type from given input path
        String[] itemParts = PathStringUtils.splitBy(itemPath, PathStringUtils.COLON)
        if (itemParts.size() != 2){
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "bad path $path")
        }
        String itemDomainType = getDomainTypeFromPathPrefix(itemParts[0])
        String subPathOnly = itemParts[1].find(PathStringUtils.DISCARD_AFTER_VERSION) ?: itemParts[1]

        String itemSubPath = subPathOnly.replaceAll(PathStringUtils.REMOVE_VERSION_DELIM, '')
        new Tuple2(itemDomainType, itemSubPath)
    }

    String getPathPrefixForDomainType(String domainType) {
        AdministeredItemCacheableRepository repo = repositoryService.administeredItemCacheableRepositories.find {
            it.handles(domainType)
        }
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, repo, "Cannot find repository for domain Type : $domainType")
        AdministeredItem domainClass = (AdministeredItem) repo.domainClass.getDeclaredConstructor().newInstance()
        return domainClass.getPathPrefix()
    }

    String getDomainTypeFromPathPrefix(String pathPrefix) {
        String domainType = pathPrefixTypeLookup.getDomainType(pathPrefix)
        if (!domainType) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Unknown path prefix $pathPrefix for modelItem ")
        }
        domainType
    }

    Item updateDerivedProperties(Item item) {
        pathRepository.readParentItems(item as AdministeredItem)
        (item as AdministeredItem).updatePath()
        (item as AdministeredItem).updateBreadcrumbs()
        AvailableActions.updateAvailableActions(item as AdministeredItem, accessControlService)
        item
    }

    protected AdministeredItem findItemForPath(String domainType, String domainPath, String versionString, String fullPath) {
        AdministeredItemCacheableRepository repository = getAdministeredItemRepository(domainType)
        List<AdministeredItem> items = repository.findAllByLabelContaining(domainPath)
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
                it.updatePath()
                it.path.pathString.contains(versionString)
            }
        }
        item
    }
}
