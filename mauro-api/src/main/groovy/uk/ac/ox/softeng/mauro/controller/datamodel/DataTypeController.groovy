package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.Item
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.service.datamodel.DataModelHelper

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataTypeController extends AdministeredItemController<DataType, DataModel> implements DataTypeApi {

    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    DataTypeController(DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository, DataTypeContentRepository dataTypeContentRepository,
                       AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, dataTypeContentRepository)
        this.dataTypeRepository = dataTypeRepository
        this.dataClassRepository = dataClassRepository
    }

    @Audit
    @Get(Paths.DATA_TYPE_ID)
    DataType show(UUID dataModelId, UUID id) {
        DataType dataType
        try {
            dataType = administeredItemRepository.findById(id)
        } catch (Exception e) {
            ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataType, "Item with id ${id.toString()} not found")
        }
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataType, "Item with id ${id.toString()} not found")
        accessControlService.checkRole(Role.READER, dataType)

        updateDerivedProperties(dataType)
        dataType = getReferenceClassProperties(dataType)
        dataType
    }

    @Audit
    @Post(Paths.DATA_TYPE_LIST)
    @Transactional
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType) {
        DataType cleanItem = super.cleanBody(dataType) as DataType
        Item parent = super.validate(cleanItem, dataModelId)
        if (cleanItem.referenceClass) {
            cleanItem.referenceClass = validatedReferenceClass(cleanItem, parent)
        }
        DataModelHelper.validateModelTypeFields(cleanItem)
        if (cleanItem.domainType == DataType.DataTypeKind.MODEL_TYPE.stringValue) {
            validateModelResource(cleanItem)
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
            getReferenceClassProperties(it)
        }
        ListResponse.from(dataTypes)
    }

    private DataClass validatedReferenceClass(DataType dataType, AdministeredItem parent) {
        DataClass referenceClass = getReferenceDataClass(dataType.referenceClass?.id)
        if (referenceClass.dataModel.id != parent.id){
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "DataClass $referenceClass.id assigned to DataType must belong to same datamodel")
        }
        DataType sameLabelInModel = dataTypeRepository.findAllByParent(parent).find {
            it.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue && it.label == dataType.label }
        if (sameLabelInModel){
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Label $dataType.label exists for ReferenceType")
        }
        referenceClass
    }

    private DataClass getReferenceDataClass(@NonNull UUID dataClassId) {
        DataClass referenceClass = dataClassRepository.findById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, referenceClass, "Cannot find reference class ")
        referenceClass
    }

    private DataType getReferenceClassProperties(DataType dataType) {
        if (dataType.domainType == DataType.DataTypeKind.REFERENCE_TYPE.stringValue) {
            dataType.referenceClass = dataClassRepository.readById(dataType.referenceClass?.id)
        }
        dataType
    }

    private void validateModelResource(DataType dataType) {
        AdministeredItem modelResource = super.readAdministeredItem(dataType.modelResourceDomainType, dataType.modelResourceId) as Model
        if (!modelResource) {
            ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item not found : $dataType.modelResourceId, $dataType.modelResourceDomainType")
        }
    }
}
