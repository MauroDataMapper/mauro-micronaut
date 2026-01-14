package org.maurodata.controller.model

import groovy.transform.CompileStatic
import io.micronaut.data.exceptions.DataAccessException
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.service.RepositoryService

@CompileStatic
trait AdministeredItemReader {

    @Inject
    RepositoryService repositoryService

    boolean existsAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        return administeredItemRepository.existsById(domainId)
    }

    AdministeredItem readAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem administeredItem = administeredItemRepository.readById(domainId) as AdministeredItem
        if (!administeredItem) throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        administeredItem
    }

    AdministeredItem findAdministeredItem(String domainType, UUID domainId) {
        AdministeredItemCacheableRepository administeredItemRepository = getAdministeredItemRepository(domainType)
        AdministeredItem administeredItem = administeredItemRepository.findById(domainId) as AdministeredItem
        if (!administeredItem) {
            new Throwable().printStackTrace()
            throw new HttpStatusException(HttpStatus.NOT_FOUND, 'AdministeredItem not found by ID')
        }
        administeredItem
    }

    AdministeredItemCacheableRepository getAdministeredItemRepository(String domainType) {
        AdministeredItemCacheableRepository administeredItemRepository = repositoryService.getAdministeredItemRepository(domainType)
        if (!administeredItemRepository) throw new HttpStatusException(HttpStatus.NOT_FOUND, "Domain type [$domainType] not found")
        administeredItemRepository
    }

}
