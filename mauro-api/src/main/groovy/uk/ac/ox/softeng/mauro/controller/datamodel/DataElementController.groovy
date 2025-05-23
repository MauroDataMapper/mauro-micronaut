package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

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

    DataElementController(DataElementCacheableRepository dataElementRepository, DataClassCacheableRepository dataClassRepository,
                          DataModelContentRepository dataModelContentRepository, DataModelCacheableRepository dataModelRepository,
                          DataTypeCacheableRepository dataTypeCacheableRepository) {
        super(DataElement, dataElementRepository, dataClassRepository, dataModelContentRepository)
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
        DataElement cleanItem = super.cleanBody(dataElement) as DataElement
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
    @Get(Paths.DATA_ELEMENT_SEARCH)
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId, @Nullable PaginationParams params = new PaginationParams()) {
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.READER, dataClass)
        List<DataElement> dataElements = dataElementRepository.readAllByDataClass_Id(dataClassId)
        dataElements.each {
            updateDerivedProperties(it)
        }
        ListResponse<DataElement>.from(dataElements, params)
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
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Datatype not found:  $dataElementDataType.id")
        }
        DataModel dataElementDTDM = dataModelRepository.readById(dataElementDataType.dataModel.id)

        DataType existingDataType = dataTypeRepository.readById(existing.dataType.id)
        DataModel existingDTDM = dataModelRepository.readById(existingDataType.dataModel.id)
        if (dataElementDTDM && dataElementDTDM.id != existingDTDM.id) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                                          "DataElement Update payload -DataType's DataModel must be same as existing dataType datamodel $existingDTDM")

        }
        existing.dataType = dataElement.dataType
        existing
    }

    @Get(Paths.DATA_ELEMENT_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleErrorOnNullObject(HttpStatus.SERVICE_UNAVAILABLE, "Doi", "Doi is not implemented")
        return null
    }
}
