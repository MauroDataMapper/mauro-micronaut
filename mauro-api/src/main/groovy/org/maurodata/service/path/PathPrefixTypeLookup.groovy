package org.maurodata.service.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository

@CompileStatic
@Slf4j
class PathPrefixTypeLookup {

    static Map<String, String> pathPrefixDomainType

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemRepositories


    PathPrefixTypeLookup(List<AdministeredItemCacheableRepository> administeredItemRepositories) {
        this.administeredItemRepositories = administeredItemRepositories
        initialisePathPrefixLookup()
    }

    protected void initialisePathPrefixLookup() {
        Map<String, String> lookup = [:]
        administeredItemRepositories.each {
            AdministeredItem domainClass = (AdministeredItem) it.domainClass.getDeclaredConstructor().newInstance()

            lookup.putIfAbsent(normalisePathPrefix(domainClass.getPathPrefix()), domainClass.getDomainType())
        }
        //special case ->VersionedFolder = folder with isVersionable() set.
        lookup.put(normalisePathPrefix('vf'), Folder.simpleName)
        pathPrefixDomainType = lookup.asImmutable()

    }

    String getDomainType(String pathPrefix) {
        pathPrefixDomainType.getOrDefault(normalisePathPrefix(pathPrefix), null)
    }

    private static String normalisePathPrefix(String pathPrefix) {
        pathPrefix?.toLowerCase(Locale.ROOT)
    }
}
