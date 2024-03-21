package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

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
