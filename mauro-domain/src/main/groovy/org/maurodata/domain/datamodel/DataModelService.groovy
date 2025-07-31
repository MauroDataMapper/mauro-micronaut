package org.maurodata.domain.datamodel

import org.maurodata.FieldConstants
import org.maurodata.domain.model.ModelService

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
        return domainType != null && domainType.toLowerCase() in [FieldConstants.DATAMODEL_LOWERCASE, FieldConstants.DATAMODELS_LOWERCASE]
    }


}
