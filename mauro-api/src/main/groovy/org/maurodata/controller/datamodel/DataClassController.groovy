package org.maurodata.controller.datamodel

import org.maurodata.api.model.CopyDataClassParamsDTO
import org.maurodata.domain.model.Item

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.datamodel.DataClassApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository

import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams
@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataClassController extends AdministeredItemController<DataClass, DataModel> implements DataClassApi {

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository

    DataModelCacheableRepository dataModelRepository

    @Inject
    DataClassController(AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository, DataModelCacheableRepository dataModelRepository) {
        super(DataClass, dataClassRepository, dataModelRepository)
        this.dataModelRepository = dataModelRepository
        this.dataClassRepository = dataClassRepository
    }

    @Audit
    @Get(Paths.DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        super.create(dataModelId, dataClass)
    }

    @Audit
    @Put(Paths.DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataClass
    )
    
    @Transactional
    @Delete(Paths.DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass dataClassToDelete = dataClassRepository.loadWithContent(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToDelete, "DataClass $id not found")
        deleteDanglingReferenceTypes(dataClassToDelete.allChildDataClasses(), dataClassToDelete.allChildDataElements())
        HttpResponse deletedResponse = super.delete(id, dataClass)
        deletedResponse
    }

    @Audit
    @Get(Paths.DATA_CLASS_SEARCH)
    ListResponse<DataClass> list(UUID dataModelId, @Nullable PaginationParams params = new PaginationParams()) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataClass> classes = dataClassRepository.readAllByDataModelAndParentDataClassIsNull(dataModel)
        classes.each {
            updateDerivedProperties(it)
        }
        ListResponse<DataClass>.from(classes,params)
    }


    @Audit
    @Get(Paths.ALL_DATA_CLASSES)
    ListResponse<DataClass> allDataClasses(@NonNull UUID dataModelId) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataClass> classes = dataClassRepository.readAllByDataModel(dataModel)
        classes.each {
            updateDerivedProperties(it)
        }
        ListResponse<DataClass>.from(classes)
    }


    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass) {

        cleanBody(dataClass)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.EDITOR, parentDataClass)
        dataClass.parentDataClass = parentDataClass
        createEntity(dataModel, dataClass)
        return dataClass
    }

    @Audit
    @Put(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'parentDataClassId',
        deletedObjectDomainType = DataClass
    )
    @Delete(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass dataClassToDelete = dataClassRepository.loadWithContent(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToDelete, "DataClass $id not found")
        deleteDanglingReferenceTypes(dataClassToDelete.allChildDataClasses(), dataClassToDelete.allChildDataElements())
        HttpResponse deletedResponse = super.delete(id, dataClass)
        deletedResponse
    }

    @Audit
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId) {
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.READER, parentDataClass)
        ListResponse.from(dataClassRepository.readAllByParentDataClass_Id(parentDataClassId))

    }

    @Audit
    @Put(Paths.DATA_CLASS_EXTENDS)
    DataClass createExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.createExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'id',
        deletedObjectDomainType = DataClass,
        description = 'Delete DataClass extends relationship'
    )
    @Delete(Paths.DATA_CLASS_EXTENDS)
    DataClass deleteExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.deleteExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }


    @Audit
    @Post(Paths.DATA_CLASS_COPY)
    @Transactional
    DataClass copyDataClass(UUID toDataModelId, UUID fromDataModelId, UUID dataClassId, @Body @Nullable CopyDataClassParamsDTO copyDataClassParams = null) {

        DataModel toDataModel = dataModelRepository.loadWithContent(toDataModelId)
        accessControlService.checkRole(Role.EDITOR, toDataModel)

        DataModel fromDataModel = dataModelRepository.loadWithContent(fromDataModelId)
        accessControlService.canDoRole(Role.READER, fromDataModel)

        // It's loaded in with the DataModel content, so find it rather than loading another copy
        DataClass fromDataClass = fromDataModel.dataClasses.find {DataClass dataClass -> dataClass.id == dataClassId}
        //verify
        if (fromDataClass == null) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Cannot find dataClass $dataClassId for dataModel $fromDataModelId")
        }
        accessControlService.canDoRole(Role.EDITOR, fromDataClass)

        // Make a deep clone, replacing fromDataModel with toDataModel throughout
        IdentityHashMap<Item, Item> replacements = new IdentityHashMap<>(256)
        replacements.put(fromDataModel, toDataModel)
        DataClass toDataClass = fromDataClass.deepClone(replacements) as DataClass

        Set<DataType> newDataTypes = copyDataTypes(toDataClass)
        toDataModel.dataTypes = newDataTypes as List
        toDataModel.dataClasses = [toDataClass]

        if(copyDataClassParams != null && copyDataClassParams.copyLabel != null && !copyDataClassParams.copyLabel.trim().isEmpty()) {
            toDataClass.label = copyDataClassParams.copyLabel.trim()
        } else {
            if (fromDataModel.id == toDataModel.id) {toDataClass.label = "${toDataClass.label} (Copy)"}
        }
        /*
            TO DO: Question about copyPermissions
            In grails copyPermissions == true is not implemented and throws an error if copyPermissions == true, which may mean
             that the permissions are not copied by default, and default permissions and ownership are applied.
            However, here in micronaut the permission properties are copied by default, including the catalogue user as this
             is not overwritten. See: contentsService.saveContentOnly() not calling contentHandler.setCreateProperties
             To implement copyPermissions would require doing nothing when copyPermissions is true, and recursively setting defaults
             otherwise
         */

        // Trigger this to be saved
        unsetDataElementIds(toDataClass)

        try {
            contentsService.saveContentOnly(toDataModel)
        } catch (Throwable th) {
            th.printStackTrace()
            throw th
        }

        updateDerivedProperties(toDataClass)

        // clean before responding
        toDataClass.dataElements = []

        toDataClass
    }

    @Get(Paths.DATA_CLASS_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    protected void deleteDanglingReferenceTypes(List<DataClass> deletedDataClassLookup, List<DataElement> allDataElements) {
        List<DataType> dataTypes = dataTypeRepository.findByReferenceClassIn(deletedDataClassLookup).unique() as List<DataType>
        List<DataElement> referencedDataElements = dataElementRepository.readAllByDataTypeIn(dataTypes)
        if (!allDataElements.id.containsAll(referencedDataElements.id)){
            // All datatypes are referenced by things that will be deleted
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass(es) referenced as ReferencedDataType in data elements")
        }
        dataTypeRepository.deleteAll(dataTypes)
    }

    protected Set<DataType> copyDataTypes(DataClass dataClass) {

        DataModel newDataModel = dataClass.dataModel

        Map<String, DataType> newDataModel_labelOntoDataType = newDataModel.dataTypes.collectEntries {DataType dataType ->
            [(dataType.label): dataType]
        }

        Map<String, DataType> oldDataModel_labelOntoDataType = dataClass.allChildDataElements().collectEntries {DataElement dataElement ->
            [(dataElement.dataType.label): dataElement.dataType]
        }

        Map<String, DataType> copied = [:]

        dataClass.allChildDataElements().each {dataElement ->
            dataElement.dataModel = newDataModel
            DataType alreadyCopied = copied.get(dataElement.dataType.label)
            if (alreadyCopied != null) {
                dataElement.dataType = alreadyCopied
            } else {
                DataType alreadyGot = newDataModel_labelOntoDataType.get(dataElement.dataType.label)
                if (alreadyGot != null) {
                    dataElement.dataType = alreadyGot
                    // Make sure it is in the list so that the DataElement can be created
                    copied.put(dataElement.dataType.label, alreadyGot)
                } else {
                    DataType needToCopy = oldDataModel_labelOntoDataType.get(dataElement.dataType.label)
                    if (needToCopy != null) {
                        needToCopy.id = null
                        needToCopy.dataModel = newDataModel
                        dataElement.dataType = needToCopy
                        if (dataElement.dataType.referenceClass) {
                            dataElement.dataType.referenceClass = findReferenceClass(dataElement.dataType.referenceClass, dataClass)
                            if (!dataElement.dataType.referenceClass) {
                                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "A data element uses a data type that refers to a dataclass not being copied")
                            }
                        }
                        copied.put(dataElement.dataType.label, needToCopy)
                    } else {
                        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass includes an element with an invalid datatype")
                    }
                }
            }
        }
        return copied.values() as Set<DataType>
    }

    protected void unsetDataElementIds(DataClass dataClass) {
        dataClass.id = null
        dataClass.dataElements.each {
            it.id = null
        }
        dataClass.dataClasses.each {
            unsetDataElementIds(it)
        }
    }

    protected DataClass findReferenceClass(DataClass referenceClass, DataClass dataClass) {
        if(referenceClass.id == dataClass.id) {
            return dataClass
        } else {
            dataClass.dataClasses.each {
                DataClass response = findReferenceClass(referenceClass, it)
                if(response) {
                    return response
                }
            }
        }
        return null
    }
}
