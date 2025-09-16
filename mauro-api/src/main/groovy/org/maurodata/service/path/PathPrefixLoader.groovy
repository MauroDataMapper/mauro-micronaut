package org.maurodata.service.path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository

@CompileStatic
@Slf4j
class PathPrefixLoader {

    static Map<String, String> pathPrefixDomainType

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemRepositories


    PathPrefixLoader(List<AdministeredItemCacheableRepository> administeredItemRepositories) {
        this.administeredItemRepositories = administeredItemRepositories
        initialisePathPrefixLookup()
    }

    protected void initialisePathPrefixLookup() {
        Map<String, String> lookup = [:]
        administeredItemRepositories.each {
            AdministeredItem domainClass = (AdministeredItem) it.domainClass.getDeclaredConstructor().newInstance()
            lookup.putIfAbsent(domainClass.getPathPrefix(), domainClass.domainType)
        }
        pathPrefixDomainType = lookup.asImmutable()

    }

    String getDomainType(String pathPrefix) {
        pathPrefixDomainType.getOrDefault(pathPrefix, null)
    }
}
