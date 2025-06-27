package org.maurodata.controller.datamodel

import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.datamodel.DataElementApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import org.maurodata.persistence.datamodel.DataElementContentRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

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
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional

@CompileStatic
@Slf4j
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class DataElementController extends AdministeredItemController<DataElement, DataClass> implements DataElementApi {

    DataElementCacheableRepository dataElementRepository

    DataClassCacheableRepository dataClassRepository

    DataModelCacheableRepository dataModelRepository

    DataTypeCacheableRepository dataTypeRepository

    DataElementContentRepository dataElementContentRepository

    DataElementController(DataElementCacheableRepository dataElementRepository, DataClassCacheableRepository dataClassRepository,
                          DataElementContentRepository dataElementContentRepository, DataModelCacheableRepository dataModelRepository,
                          DataTypeCacheableRepository dataTypeCacheableRepository) {
        super(DataElement, dataElementRepository, dataClassRepository, dataElementContentRepository)
        this.dataElementRepository = dataElementRepository
        this.dataModelRepository = dataModelRepository
        this.dataClassRepository = dataClassRepository
        this.dataTypeRepository = dataTypeCacheableRepository
    }

    @Audit
    @Get(Paths.DATA_ELEMENT_ID)
    DataElement show(UUID dataModelId, UUID dataClassId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_ELEMENT_LIST)
    DataElement create(UUID dataModelId, UUID dataClassId, @Body @NonNull DataElement dataElement) {
        cleanBody(dataElement)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.EDITOR, dataClass)
        dataElement.dataClass = dataClass
        createEntity(dataClass, dataElement)
        return dataElement

    }

    @Audit
    @Put(Paths.DATA_ELEMENT_ID)
    @Transactional
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement) {
        DataElement cleanItem = super.cleanBody(dataElement, false) as DataElement
        DataElement existing = administeredItemRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, existing)
        boolean hasChanged = updateProperties(existing, cleanItem)
        if (!hasChanged && dataElement?.dataType?.id != existing.dataType?.id) hasChanged = true
        existing = validateDataTypeChange(existing, dataElement)
        updateDerivedProperties(existing)
        DataElement updated = existing
        if (hasChanged) {
            updated = administeredItemRepository.update(existing)
            updated = updateClassifiers(updated)
        }
        updated
    }

    @Audit(
        parentDomainType = DataClass,
        parentIdParamName = 'dataClassId',
        deletedObjectDomainType = DataElement
    )
    @Delete(Paths.DATA_ELEMENT_ID)
    HttpResponse delete(UUID dataModelId, UUID dataClassId, UUID id, @Body @Nullable DataElement dataElement) {
        super.delete(id, dataElement)
    }

    @Audit
    @Get(Paths.DATA_ELEMENT_LIST_PAGED)
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId, @Nullable PaginationParams params = new PaginationParams()) {

        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.READER, dataClass)
        List<DataElement> dataElements = dataElementRepository.readAllByDataClass_Id(dataClassId)

        ListResponse<DataElement> theList = ListResponse<DataElement>.from(dataElements, params)

        theList.items.each {Object that ->
            DataElement it = (DataElement) that
            updateDerivedProperties(it)
            if (it.dataType) {
                it.dataType = dataTypeRepository.readById(it.dataType.id)
            }
        }

        return theList
    }

    /**
     * DataType in DataElement Payload can be changed, but the DT must exist.
     * Furthermore, the DT must have the same DM as the existing DT's DM. This update does not update the DataType
     * @param existing
     * @param dataElement
     * @return existing, updated with new DTid
     */
    private DataElement validateDataTypeChange(DataElement existing, DataElement dataElement) {
        if (!dataElement.dataType) return existing

        DataType dataElementDataType = dataTypeRepository.readById(dataElement.dataType.id)
        if (!dataElementDataType) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Datatype not found:  $dataElementDataType.id")
        }
        DataModel dataElementDTDM = dataModelRepository.readById(dataElementDataType.dataModel.id)

        DataType existingDataType = dataTypeRepository.readById(existing.dataType.id)
        DataModel existingDTDM = dataModelRepository.readById(existingDataType.dataModel.id)
        if (dataElementDTDM && dataElementDTDM.id != existingDTDM.id) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                                          "DataElement Update payload -DataType's DataModel must be same as existing dataType datamodel $existingDTDM")

        }
        existing.dataType = dataElement.dataType
        existing
    }

    @Get(Paths.DATA_ELEMENT_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Doi is not implemented")
        return null
    }
}
