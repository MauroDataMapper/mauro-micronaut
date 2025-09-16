package org.maurodata.service.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.controller.model.AvailableActions
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.security.AccessControlService

@CompileStatic
@Slf4j
class PathService {

    static final String VERTICAL_BAR_ESCAPE = "\\|"
    static final String COLON = ":"

    @Inject
    PathPrefixLoader pathPrefixLoader

    @Inject
    PathRepository pathRepository

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemRepositories

    @Inject
    AccessControlService accessControlService

    /**
     *
     *
     * @param domainType eg folders
     * @param path eg "%3Asoluta%20eum%20architecto%7Cdm%3ABadgerNet%20UK%20Maternity%24main" ("fo:soluta eum architecto|dm:BadgerNet UK Maternity$main")
     *  find last path after |.  remove text after $(branch)
     * @return path (label in table)
     */
    String getPathPrefixSubPath(String pathPrefix, String fullPath) {
        String[] pathSubPaths = splitBy(fullPath, VERTICAL_BAR_ESCAPE)
        String entireSubPath = pathSubPaths.find {it.startsWith("$pathPrefix:")}
        if (!entireSubPath ) ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Path starting with $pathPrefix not found")
        String subPath =  entireSubPath - "$pathPrefix:"
        def result = subPath.find(~/.*\$/)  //find up to and including $ eg branch
        result - '$'
    }


    String getPathPrefixForDomainType(String domainType) {
        AdministeredItemCacheableRepository repo = administeredItemRepositories.find {
            it.handles(domainType)
        }
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, repo, "Cannot find repository for domain Type : $domainType")
        AdministeredItem domainClass = (AdministeredItem) repo.domainClass.getDeclaredConstructor().newInstance()
        return domainClass.getPathPrefix()
    }

    String getDomainTypeFromPathPrefix(String pathPrefix) {
        String domainType = pathPrefixLoader.getDomainType(pathPrefix)
        if (!domainType) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Unknown path prefix $pathPrefix for modelItem ")
        }
        domainType
    }

    String getItemPath(String path) {
        splitBy(path, VERTICAL_BAR_ESCAPE).last()
    }

   String[] splitBy(String path, String separator){
        path.split(separator)

    }

    Item updateDerivedProperties(Item item) {
        pathRepository.readParentItems(item as AdministeredItem)
        (item as AdministeredItem).updatePath()
        (item as AdministeredItem).updateBreadcrumbs()
        AvailableActions.updateAvailableActions(item as AdministeredItem, accessControlService)
        item
    }
}
