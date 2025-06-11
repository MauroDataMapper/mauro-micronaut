package org.maurodata.persistence.datamodel


import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import org.maurodata.persistence.model.AdministeredItemContentRepository

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class EnumerationValueContentRepository extends AdministeredItemContentRepository {

    @Inject
    EnumerationValueCacheableRepository enumerationValueCacheableRepository

    @Override
    EnumerationValue readWithContentById(UUID id) {
        EnumerationValue enumerationValue = enumerationValueCacheableRepository.readById(id)

        enumerationValue
    }
}
