package org.maurodata.controller.datamodel

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.micronaut.http.exceptions.HttpStatusException
import org.maurodata.api.model.CopyDataClassParamsDTO
import org.maurodata.domain.model.AdministeredItem
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
    @Operation(operationId = 'showDataClass', summary = "Get a data class", description = "Returns a data class.")
    @Get(Paths.DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(operationId = 'createDataClass', summary = "Create a data class", description = "Creates a data class.")
    @Post(Paths.DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        super.create(dataModelId, dataClass)
    }

    @Audit
    @Operation(operationId = 'updateDataClass', summary = "Update a data class", description = "Updates a data class.")
    @Put(Paths.DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        DataClass existing = dataClassRepository.readById(id)
        if (existing == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }
        if(dataClass.parentDataClass) {
            guardAgainstBadMoves(id, dataClass.parentDataClass)
        }
        super.update(id, dataClass)
    }

    @Audit
    @Put(Paths.DATA_CLASS_MOVE)
    DataClass moveDataClass(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass existing = dataClassRepository.readById(id)
        if (existing == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Object not found")
        }
        if(dataClass.parentDataClass) { // Moving to inside another class
            guardAgainstBadMoves(id, dataClass.parentDataClass)
            return update(dataModelId, id, dataClass)
        } else { // Moving to the top-level DataModel
            accessControlService.checkRole(Role.EDITOR, existing)
            existing.parentDataClass = null
            DataClass updated = dataClassRepository.update(existing)
            return updated
        }
    }




    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataClass
    )
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteDataClass', summary = "Delete a data class", description = "Deletes a data class.")
    @Delete(Paths.DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass dataClassToDelete = dataClassRepository.loadWithContent(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToDelete, "DataClass $id not found")
        pathRepository.readParentItems(dataClassToDelete)
        deleteDanglingReferenceTypes(dataClassToDelete.allChildDataClasses(), dataClassToDelete.allChildDataElements())
        HttpResponse deletedResponse = super.delete(id, dataClass)
        deletedResponse
    }

    @Audit
    @Operation(operationId = 'listDataClassPaged', summary = "List the data classes", description = "Returns the data classes. You must have read privileges on the item in question.")
    @Get(Paths.DATA_CLASS_SEARCH)
    ListResponse<DataClass> list(UUID dataModelId, @Nullable PaginationParams params = new PaginationParams()) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        ListResponse<DataClass> classes = dataClassRepository.readListResponseByDataModelAndParentDataClassIsNull(dataModel, params)
        classes.items.each {
            updateDerivedProperties(it)
        }
        classes
    }


    @Audit
    @Operation(summary = "List the data classes", description = "Returns the data classes. You must have read privileges on the item in question.")
    @Get(Paths.ALL_DATA_CLASSES_PAGED)
    ListResponse<DataClass> allDataClasses(@NonNull UUID dataModelId, @Nullable PaginationParams params = new PaginationParams()) {
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.READER, dataModel)
        List<DataClass> classes = dataClassRepository.readAllByDataModel(dataModel)
        classes.each {
            updateDerivedProperties(it)
        }
        ListResponse<DataClass>.from(classes, params)
    }


    @Audit
    @Operation(operationId = 'showDataClassChild', summary = "Get a data class", description = "Returns a data class.")
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(id)
    }

    @Audit
    @Operation(summary = "Create a data class", description = "Creates a data class. You must have edit privileges on the item in question.")
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
    @Operation(operationId = 'updateDataClassChild', summary = "Update a data class", description = "Updates a data class.")
    @Put(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'parentDataClassId',
        deletedObjectDomainType = DataClass
    )
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteDataClassChild', summary = "Delete a data class", description = "Deletes a data class.")
    @Delete(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        DataClass dataClassToDelete = dataClassRepository.loadWithContent(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToDelete, "DataClass $id not found")
        deleteDanglingReferenceTypes(dataClassToDelete.allChildDataClasses(), dataClassToDelete.allChildDataElements())
        HttpResponse deletedResponse = super.delete(id, dataClass)
        deletedResponse
    }

    @Audit
    @Operation(operationId = 'listDataClassChild', summary = "List the data classes", description = "Returns the data classes. You must have read privileges on the item in question.")
    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST_PAGED)
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId, @Nullable PaginationParams params = new PaginationParams()) {
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        accessControlService.checkRole(Role.READER, parentDataClass)
        ListResponse.from(dataClassRepository.readAllByParentDataClass_Id(parentDataClassId), params)

    }

    @Audit
    @Operation(summary = "Update a data class", description = "Updates a data class. You must have edit privileges on the item in question.")
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
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a data class", description = "Deletes a data class. You must have edit privileges on the item in question.")
    @Delete(Paths.DATA_CLASS_EXTENDS)
    DataClass deleteExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId) {
        DataClass sourceDataClass = dataClassRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, sourceDataClass)
        DataClass targetDataClass = dataClassRepository.readById(otherClassId)
        dataClassRepository.deleteExtensionRelationship(sourceDataClass, targetDataClass)
        dataClassRepository.findById(id)
    }


    @Audit
    @Operation(summary = "Copy the data class", description = "Copies the data class. You must have read or edit privileges on the item in question, depending on the action.")
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

    @Audit
    @Post(Paths.DATA_CLASS_COPY_TO_CLASS)
    @Transactional
    DataClass copyDataClass(UUID toDataModelId, UUID toDataClassId, UUID fromDataModelId, UUID dataClassId, @Body @Nullable CopyDataClassParamsDTO copyDataClassParams = null) {

        DataModel toDataModel = dataModelRepository.loadWithContent(toDataModelId)
        accessControlService.checkRole(Role.EDITOR, toDataModel)

        DataModel fromDataModel = dataModelRepository.loadWithContent(fromDataModelId)
        accessControlService.canDoRole(Role.READER, fromDataModel)

        DataClass toDataClassParent = toDataModel.dataClasses.find {DataClass dataClass -> dataClass.id == toDataClassId}
        //verify
        if (toDataClassParent == null) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Cannot find dataClass $toDataClassId for dataModel $toDataModelId")
        }
        accessControlService.canDoRole(Role.EDITOR, toDataClassParent)

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
        toDataClass.parentDataClass = toDataClassParent
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

    protected void guardAgainstBadMoves(UUID dataClassId, DataClass parentDataClass) {
        if(parentDataClass) {
            if(!parentDataClass.id) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot move data class - no id for parent set!")
            }
            if(parentDataClass.id == dataClassId) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot move data class to inside itself")
            }
            List<AdministeredItem> allParents = pathRepository.readParentItems(parentDataClass)
            if(allParents.find {it.id == dataClassId}) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot move data class inside one of its children!")
            }
        }
    }
}
