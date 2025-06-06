package uk.ac.ox.softeng.mauro.service.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.security.AccessControlService

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.http.HttpStatus
import jakarta.inject.Inject

@CompileStatic
class DataTypeService {
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository
    AccessControlService accessControlService

    @Inject
    DataTypeService(AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository,
                    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository,
                   AccessControlService accessControlService) {
        this.dataTypeRepository = dataTypeRepository
        this.dataClassRepository = dataClassRepository
        this.accessControlService = accessControlService
    }

    DataType validateDataType(DataType dataType, AdministeredItem parent) {
        if (dataType.isReferenceType()) {
            dataType.referenceClass = validatedReferenceClass(dataType, parent)
        }
        DataModelHelper.validateModelTypeFields(dataType)
        dataType
    }

   DataType getReferenceClassProperties(DataType dataType) {
       if (dataType.isReferenceType()) {
           dataType.referenceClass = dataClassRepository.readById(dataType.referenceClass?.id)
       }
        dataType
    }

    protected DataClass validatedReferenceClass(DataType dataType, AdministeredItem parent) {
        UUID referenceClassId = dataType.referenceClass?.id
        if ( !referenceClassId){
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Error -ReferenceType datatype requires referenceClass")
        }
        DataClass referenceClass = getReferenceDataClass(referenceClassId)
        accessControlService.checkRole(Role.READER, referenceClass)
        if (referenceClass.dataModel.id != parent.id) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass $referenceClass.id assigned to DataType must belong to same datamodel")
        }
        DataType sameLabelInModel = dataTypeRepository.findAllByParent(parent).find {
            it.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue && it.label == dataType.label
        }
        if (sameLabelInModel) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Label $dataType.label exists for ReferenceType")
        }
        referenceClass
    }

    protected DataClass getReferenceDataClass(@NonNull UUID dataClassId) {
        DataClass referenceClass = dataClassRepository.findById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, referenceClass, "Cannot find reference class ")
        referenceClass
    }

}
