package org.maurodata.controller.datamodel

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
import org.maurodata.persistence.datamodel.DataTypeContentRepository
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
    EnumerationValueRepository enumerationValueRepository

    @Inject
    RepositoryService repositoryService

    final DataTypeService dataTypeService

    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    DataTypeController(DataTypeService dataTypeService, DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository,
                       DataTypeContentRepository dataTypeContentRepository,
                       AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, dataTypeContentRepository)
        this.dataTypeService = dataTypeService
        this.dataTypeRepository = dataTypeRepository
        this.dataClassRepository = dataClassRepository
    }

    @Audit
    @Get(Paths.DATA_TYPE_ID)
    DataType show(UUID dataModelId, UUID id) {
        DataType dataType
        dataType = administeredItemRepository.findById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataType, "Item with id ${id.toString()} not found")
        accessControlService.checkRole(Role.READER, dataType)

        updateDerivedProperties(dataType)
        dataTypeService.getReferenceClassProperties(dataType)
        dataTypeService.getEnumerationValues(dataType)
    }

    @Audit
    @Post(Paths.DATA_TYPE_LIST)
    @Transactional
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType) {

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

            if (dataTypeVersionedFolder != null) {System.out.println(dataTypeVersionedFolder.toString());}

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

        if (dataType.enumerationValues) {
            dataType.enumerationValues.each {enumValue ->
                enumValue.enumerationType = (DataType) dataType
                enumerationValueRepository.save(enumValue)
            }
        }
        created
    }

    @Audit
    @Put(Paths.DATA_TYPE_ID)
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType) {
        super.update(id, dataType)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataType
    )
    @Delete(Paths.DATA_TYPE_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType) {
        super.delete(id, dataType)
    }

    @Audit
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
