package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataElement
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class DataElementContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataElementCacheableRepository dataElementCacheableRepository

    @Override
    DataElement readWithContentById(UUID id) {
        DataElement dataElement = dataElementCacheableRepository.readById(id)
        dataElement
    }
}
