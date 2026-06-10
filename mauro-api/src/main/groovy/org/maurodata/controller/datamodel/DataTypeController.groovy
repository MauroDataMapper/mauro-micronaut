package org.maurodata.controller.datamodel

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.model.Path
import org.maurodata.persistence.cache.ModelCacheableRepository

import groovy.transform.CompileStatic
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
import org.maurodata.api.datamodel.DataTypeApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository

import org.maurodata.persistence.datamodel.EnumerationValueRepository
import org.maurodata.service.datamodel.DataTypeService
import org.maurodata.web.ListResponse
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.service.RepositoryService
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataTypeController extends AdministeredItemController<DataType, DataModel> implements DataTypeApi {

    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    @Inject
    RepositoryService repositoryService

    final DataTypeService dataTypeService

    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    DataTypeController(DataTypeService dataTypeService, DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository,
                       AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository) {
        super(DataType, dataTypeRepository, dataModelRepository)
        this.dataTypeService = dataTypeService
        this.dataTypeRepository = dataTypeRepository
        this.dataClassRepository = dataClassRepository
    }

    @Audit
    @Operation(operationId = 'showDataType', summary = "Get a data type", description = "Returns a data type. You must have read privileges on the item in question.")
    @Get(Paths.DATA_TYPE_ID)
    DataType show(UUID dataModelId, UUID id) {
        DataType dataType
        dataType = administeredItemRepository.findById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataType, "Item with id ${id} not found")
        accessControlService.checkRole(Role.READER, dataType)

        updateDerivedProperties(dataType)
        dataTypeService.getReferenceClassProperties(dataType)
        dataTypeService.getEnumerationValues(dataType)
    }

    @Audit
    @Operation(operationId = 'createDataType', summary = "Create a data type", description = "Creates a data type.")
    @Post(Paths.DATA_TYPE_LIST)
    @Transactional
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType) {
        DataModel dataModel = dataModelRepository.findById(dataModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataModel, "DataModel with id ${dataModelId} not found")
        accessControlService.checkRole(Role.EDITOR, dataModel)

        // Pull out the enumeration values for saving separately as they require the dataType to be saved first

        List<EnumerationValue> enumerationValues = []
        if(dataType.enumerationValues)
        {
            enumerationValues.addAll(dataType.enumerationValues)
            dataType.enumerationValues = []
        }
        DataType cleanItem = super.cleanBody(dataType) as DataType
        Item parent = super.validate(cleanItem, dataModelId)
        cleanItem = dataTypeService.validateDataType(cleanItem, parent)

        if (cleanItem.isReferenceType()) {
            if (cleanItem.referenceClass.id == parent.id) {
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Data class element shouldn't reference it")
            }
        }
        if (dataType.isModelType()) {

            // Either the dataType is finalised
            // or this model and the data type are in the
            // same versioned folder

            ModelCacheableRepository domainRepository = repositoryService.getModelRepository(dataType.modelResourceDomainType)

            AdministeredItem dataTypeAdministeredItem = (AdministeredItem) domainRepository.readById(dataType.modelResourceId)

            pathRepository.readParentItems(dataTypeAdministeredItem)
            pathRepository.readParentItems(parent)

            Path pathToDataType = dataTypeAdministeredItem.getPathToEdge()
            Path pathToDataModel = parent.getPathToEdge()

            Item dataTypeVersionedFolder = pathToDataType.findAncestorNodeItem(dataType.modelResourceId, "VersionedFolder")
            Item dataModelVersionedFolder = null

            if (dataTypeVersionedFolder != null) {
                dataModelVersionedFolder = pathToDataModel.findAncestorNodeItem(parent.id, "VersionedFolder")
            }
            if (dataTypeVersionedFolder == null || dataModelVersionedFolder == null || dataTypeVersionedFolder.id != dataModelVersionedFolder.id) {
                validateModelResource(dataType)
            }
        }

        // give it a label
        if (!cleanItem.label) {
            if (cleanItem.isReferenceType()) {
                cleanItem.label = "Reference to ${cleanItem.referenceClass.label}"
            } else if (cleanItem.modelResourceId) {
                final UUID modelResourceId = cleanItem.modelResourceId
                final String modelResourceDomainType = cleanItem.modelResourceDomainType

                AdministeredItemCacheableRepository repository = repositoryService.getAdministeredItemRepository(modelResourceDomainType)

                AdministeredItem item = (AdministeredItem) repository.readById(modelResourceId)
                cleanItem.label = "Reference to ${item.label}"
            }
        }

        DataType created = super.createEntity(parent, cleanItem) as DataType
        created = super.validateAndAddClassifiers(created) as DataType

        if (enumerationValues) {
            enumerationValues.each {enumValue ->
                enumValue.enumerationType = dataType
                enumerationValueRepository.save(enumValue)
            }
        }
        created
    }

    @Audit
    @Operation(operationId = 'updateDataType', summary = "Update a data type", description = "Updates a data type.")
    @Put(Paths.DATA_TYPE_ID)
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType) {
        super.update(id, dataType)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataType
    )
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(operationId = 'deleteDataType', summary = "Delete a data type", description = "Deletes a data type.")
    @Delete(Paths.DATA_TYPE_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType) {
        super.delete(id, dataType)
    }

    @Audit
    @Operation(operationId = 'listDataTypePaged', summary = "List the data types", description = "Returns the data types. You must have read privileges on the item in question.")
    @Get(Paths.DATA_TYPE_LIST_PAGED)
    ListResponse<DataType> list(UUID dataModelId, @Nullable PaginationParams params = new PaginationParams()) {
        Item parent = parentItemRepository.readById(dataModelId)
        if (!parent) return null
        accessControlService.checkRole(Role.READER, parent)
        List<DataType> dataTypes = administeredItemRepository.readAllByParent(parent)
        dataTypes.each {
            updateDerivedProperties(it)
            dataTypeService.getReferenceClassProperties(it)
            dataTypeService.getEnumerationValues(it)
        }
        ListResponse.from(dataTypes, params)
    }

    @Operation(summary = "List the data types", description = "Returns the data types. You must have read privileges on the item in question.")
    @Get(Paths.DATA_TYPE_DATA_ELEMENTS_PAGED)
    ListResponse<DataElement> listDataElementsForType(UUID dataModelId, UUID dataTypeId, @Nullable PaginationParams params = new PaginationParams()) {
        DataType dataType
        dataType = administeredItemRepository.findById(dataTypeId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataType, "Item with id ${dataTypeId} not found")
        accessControlService.checkRole(Role.READER, dataType)

        List<DataElement> dataElements = dataElementRepository.readAllByDataTypeIn([dataType])
        ListResponse.from(dataElements, params)
    }

    @Operation(summary = "Get a data type", description = "Returns a data type.")
    @Get(Paths.PRIMITIVETYPE_DOI)
    Map primitiveTypeDoi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Operation(summary = "Get a data type", description = "Returns a data type.")
    @Get(Paths.ENUMERATIONTYPE_DOI)
    Map enumerationTypeDoi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }

    @Operation(summary = "Get a data type", description = "Returns a data type.")
    @Get(Paths.REFERENCETYPE_DOI)
    Map referenceTypeDoi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }


    protected void validateModelResource(DataType dataType) {
        AdministeredItem modelResource = super.readAdministeredItem(dataType.modelResourceDomainType, dataType.modelResourceId) as Model
        if (!modelResource) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item not found : $dataType.modelResourceId, $dataType.modelResourceDomainType")
        }
        accessControlService.checkRole(Role.READER, modelResource)
        if (!modelResource.finalised) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Model resource is not finalised: $dataType.modelResourceId, $dataType.modelResourceDomainType")
        }
    }
}
