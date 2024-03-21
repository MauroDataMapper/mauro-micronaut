package uk.ac.ox.softeng.mauro.domain.datamodel

import uk.ac.ox.softeng.mauro.domain.model.ModelService

import groovy.transform.CompileStatic
import jakarta.inject.Singleton

/**
 * The DataModelService class provides utility functions for manipulating DataModel objects
 */
@CompileStatic
@Singleton
class DataModelService extends ModelService<DataModel> {

    Boolean handles(Class clazz) {
        clazz == DataModel
    }

    Boolean handles(String domainType) {
        domainType.toLowerCase() in ['datamodel', 'datamodels']
    }


}
