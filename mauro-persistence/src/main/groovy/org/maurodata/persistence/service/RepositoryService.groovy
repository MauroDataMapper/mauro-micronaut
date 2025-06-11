package org.maurodata.persistence.service

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository

@CompileStatic
@Singleton
class RepositoryService {

    @Inject
    List<ItemCacheableRepository> cacheableRepositories

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemCacheableRepositories

    @Inject
    List<ModelCacheableRepository> modelCacheableRepositories

    ItemCacheableRepository getRepository(Class clazz) {
        cacheableRepositories.find {it.handles(clazz)}
    }

    ItemCacheableRepository getRepository(String domainType) {
        cacheableRepositories.find {it.handles(domainType)}
    }

    AdministeredItemCacheableRepository getAdministeredItemRepository(Class clazz) {
        administeredItemCacheableRepositories.find {it.handles(clazz)}
    }

    AdministeredItemCacheableRepository getAdministeredItemRepository(String domainType) {
        administeredItemCacheableRepositories.find {it.handles(domainType)}
    }

    ModelCacheableRepository getModelRepository(Class clazz) {
        modelCacheableRepositories.find {it.handles(clazz)}
    }

    ModelCacheableRepository getModelRepository(String domainType) {
        modelCacheableRepositories.find {it.handles(domainType)}
    }
}
