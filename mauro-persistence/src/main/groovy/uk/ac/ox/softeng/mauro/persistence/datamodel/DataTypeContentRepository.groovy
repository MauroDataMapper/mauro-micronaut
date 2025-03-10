package uk.ac.ox.softeng.mauro.persistence.datamodel

import groovy.transform.CompileStatic
import jakarta.inject.Inject
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository

@CompileStatic
@Singleton
class DataTypeContentRepository extends AdministeredItemContentRepository {

    @Inject
    DataTypeCacheableRepository dataTypeCacheableRepository

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    @Override
    DataType readWithContentById(UUID id) {
        DataType dataType = dataTypeCacheableRepository.readById(id)

        dataType
    }

    List<DataType> findAllWithContentByParent(DataModel dataModel) {
        List<DataType> dataTypes = dataTypeCacheableRepository.findAllByParent(dataModel)

        Map<UUID, DataType> dataTypeMap = dataTypes.collectEntries {[it.id, it]}
        if (dataTypes) {
            Set<EnumerationValue> enumerationValues = enumerationValueRepository.findAllByEnumerationTypeIn(dataTypes)
            enumerationValues.each {enumerationValue ->
                enumerationValue.enumerationType = dataTypeMap[enumerationValue.enumerationType.id]
                enumerationValue.enumerationType.enumerationValues.add(enumerationValue)
                enumerationValue.dataModel = dataModel
            }
        }

        dataTypes
    }
}
