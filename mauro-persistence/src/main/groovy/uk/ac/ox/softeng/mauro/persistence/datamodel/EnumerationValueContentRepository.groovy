package uk.ac.ox.softeng.mauro.persistence.datamodel


import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

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
