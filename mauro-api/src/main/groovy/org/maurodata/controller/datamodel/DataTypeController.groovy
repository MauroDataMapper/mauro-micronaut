package org.maurodata.controller.datamodel


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

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataTypeController extends AdministeredItemController<DataType, DataModel> implements DataTypeApi {

    DataTypeCacheableRepository dataTypeRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    final DataTypeService dataTypeService

    @Inject
    DataTypeController(DataTypeService dataTypeService, DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository, DataTypeContentRepository dataTypeContentRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, dataTypeContentRepository)
        this.dataTypeService = dataTypeService
        this.dataTypeRepository = dataTypeRepository
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
    }

    @Audit
    @Post(Paths.DATA_TYPE_LIST)
    @Transactional
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType) {
        DataType cleanItem = super.cleanBody(dataType) as DataType
        Item parent = super.validate(cleanItem, dataModelId)
        cleanItem = dataTypeService.validateDataType(cleanItem, parent)
        if (dataType.isModelType()) {
            validateModelResource(dataType)
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
    @Get(Paths.DATA_TYPE_LIST)
    ListResponse<DataType> list(UUID dataModelId) {
        Item parent = parentItemRepository.readById(dataModelId)
        if (!parent) return null
        accessControlService.checkRole(Role.READER, parent)
        List<DataType> dataTypes = administeredItemRepository.readAllByParent(parent)
        dataTypes.each {
            updateDerivedProperties(it)
            dataTypeService.getReferenceClassProperties(it)
        }
        ListResponse.from(dataTypes)
    }

    protected void validateModelResource(DataType dataType) {
        AdministeredItem modelResource = super.readAdministeredItem(dataType.modelResourceDomainType, dataType.modelResourceId) as Model
        if (!modelResource) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item not found : $dataType.modelResourceId, $dataType.modelResourceDomainType")
        }
        accessControlService.checkRole(Role.READER, modelResource)
        if (!modelResource.finalised){
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Model resource is not finalised: $dataType.modelResourceId, $dataType.modelResourceDomainType")
        }
    }
}
