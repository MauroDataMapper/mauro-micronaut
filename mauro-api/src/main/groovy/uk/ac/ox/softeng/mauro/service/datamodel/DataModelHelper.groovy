package uk.ac.ox.softeng.mauro.service.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus

@CompileStatic
class DataModelHelper {

    static void validateModelTypeFields(DataType dataType) {
        if (isModelType(dataType)) {
            if (!dataType.modelResourceDomainType && ! dataType.modelResourceId){
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Check DataType modelResource fields: $dataType.modelResourceDomainType, $dataType.modelResourceId")
            }
            if (!isValidModelDomainType(dataType.modelResourceDomainType)) {
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Check DataType modelResource fields: $dataType.modelResourceDomainType")
            }
        } else {
            if (dataType.modelResourceId || dataType.modelResourceDomainType) {
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Model resource Id or domainType not valid for domainType $dataType.domainType")
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
