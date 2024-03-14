package uk.ac.ox.softeng.mauro.persistence.datamodel

import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.persistence.model.ModelContentRepository

import groovy.transform.CompileStatic
import io.micronaut.context.annotation.Bean
import jakarta.inject.Inject

@CompileStatic
@Bean
class DataModelContentRepository extends ModelContentRepository<DataModel> {

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    DataTypeRepository dataTypeRepository

    @Inject
    DataElementRepository dataElementRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    DataModel findWithAssociations(UUID id) {
        DataModel dataModel = dataModelRepository.findById(id)
        dataModel.dataClasses = dataClassRepository.findAllByParent(dataModel)
        if(dataModel.dataClasses) {
            dataElementRepository.findAllByDataClassIn(dataModel.dataClasses).each {dataElement ->

            }
        }

        dataModel.dataTypes = dataTypeRepository.findAllByParent(dataModel)
        dataModel
    }
}
