package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Join
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Singleton
class DataTypeContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Override
    @Join(value = 'enumerationValues', type = Join.Type.LEFT_FETCH)
    DataType readWithContentById(UUID id) {
        DataType dataType = dataTypeCacheableRepository.readById(id)

        dataType
    }
}
