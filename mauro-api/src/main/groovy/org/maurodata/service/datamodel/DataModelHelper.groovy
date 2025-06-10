package org.maurodata.service.datamodel

import org.maurodata.ErrorHandler
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus

@CompileStatic
class DataModelHelper {

    static void validateModelTypeFields(DataType dataType) {
        if (isModelType(dataType)) {
            if (!dataType.modelResourceDomainType && !dataType.modelResourceId) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Check DataType modelResource fields: $dataType.modelResourceDomainType, $dataType.modelResourceId")
            }
            if (!isValidModelDomainType(dataType.modelResourceDomainType)) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Check DataType modelResource fields: $dataType.modelResourceDomainType")
            }
        } else {
            if (dataType.modelResourceId || dataType.modelResourceDomainType) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Model resource Id or domainType not valid for domainType $dataType.domainType")
            }
        }
    }

    static Boolean isValidModelDomainType(String modelResourceDomainType) {
        switch (modelResourceDomainType.toLowerCase()) {
            case [Folder.class.simpleName.toLowerCase(), CodeSet.class.simpleName.toLowerCase(), DataModel.class.simpleName.toLowerCase(),
                  Terminology.class.simpleName.toLowerCase()] ->
                true
            default -> false
        }
    }

    static boolean isModelType(DataType dataType) {
        dataType.domainType == DataType.DataTypeKind.MODEL_TYPE.stringValue || dataType.dataTypeKind == DataType.DataTypeKind.MODEL_TYPE
    }

}
