package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
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
import jakarta.inject.Inject

@CompileStatic
@Slf4j
@Controller('/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements')
@Secured(SecurityRule.IS_ANONYMOUS)
class DataElementController extends AdministeredItemController<DataElement, DataClass> {

    DataElementCacheableRepository dataElementRepository

    DataClassCacheableRepository dataClassRepository

    DataModelCacheableRepository dataModelRepository

    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataTypeController dataTypeController
    @Inject
    DataElementController(DataElementCacheableRepository dataElementRepository, DataClassCacheableRepository dataClassRepository,
                          DataModelContentRepository dataModelContentRepository, DataModelCacheableRepository dataModelRepository,
                          DataTypeCacheableRepository dataTypeRepository) {
        super(DataElement, dataElementRepository, dataClassRepository, dataModelContentRepository)
        this.dataModelRepository = dataModelRepository
        this.dataElementRepository = dataElementRepository
        this.dataClassRepository = dataClassRepository
        this.dataTypeRepository = dataTypeRepository
    }

    @Get('/{id}')
    DataElement show(UUID dataModelId, UUID dataClassId, UUID id) {
        DataElement dataElement = super.show(id) as DataElement
        handleError(HttpStatus.NOT_FOUND, dataElement.dataType, "dataElement $dataElement.id  is missing dataType")
        DataType dataType = dataTypeRepository.readById(dataElement.dataType?.id)
        dataElement.dataType = dataType
        dataElement
    }

    @Post
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

    @Transactional
    @Put('/{id}')
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement) {
        DataElement cleanItem = super.cleanBody(dataElement) as DataElement
        DataElement existing = administeredItemRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, existing)
        verifyValidPayload(existing, cleanItem)
        DataElement updated = updateDataType(existing, cleanItem)
        updated = updateEntity(existing, cleanItem)
        updated = updateClassifiers(updated)
        updated
    }

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID dataClassId, UUID id, @Body @Nullable DataElement dataElement) {
        super.delete(id, dataElement)
    }

    @Get
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId) {
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.READER, dataClass)
        ListResponse.from(dataElementRepository.readAllByDataClass_Id(dataClassId))
    }


    private DataElement updateDataType(DataElement existing, DataElement dataElementToUpdate) {
        DataType existingDataType
        if (dataElementToUpdate.dataType?.id) {
            existingDataType = dataTypeRepository.readById(dataElementToUpdate.dataType.id)
            handleError(HttpStatus.NOT_FOUND, existingDataType, "Datatype not found $dataElementToUpdate.dataType.id")
            dataElementToUpdate.dataType.id = null //clear for cleanBody
            DataType cleanItem = dataTypeController.cleanBody(dataElementToUpdate.dataType)

            boolean hasChanged = dataTypeController.updateProperties(existingDataType, cleanItem)
            dataTypeController.updateDerivedProperties(existingDataType)
            DataType result = existingDataType
            if (hasChanged) {
                result = dataTypeRepository.update(existingDataType)
                dataElementRepository.invalidate(existing) //invalidate cache
            }
            dataElementToUpdate.dataType = result
        }
        dataElementToUpdate
    }

    /**
     * Check datatypes are valid
     * dataType must belong to same dataModel and exist

     * throws RunTime Exception
     */
    private void verifyValidPayload(DataElement existing, DataElement proposed) {
        if (!proposed.dataType) return
        DataType proposedDataType = dataTypeRepository.readById(proposed.dataType.id)
        if (!proposedDataType) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Datatype not found:  $proposedDataType.id")
        }
        DataModel proposedParent = dataModelRepository.readById(proposedDataType.dataModel.id)

        if (!existing.dataType) return

        DataType existingDataType = dataTypeRepository.readById(existing.dataType.id)
        if (!existingDataType) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Datatype not found:  $existingDataType.id")
        }
        DataModel existingParent = dataModelRepository.readById(existingDataType.dataModel.id)

        if (existingParent.id != proposedParent.id) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                                          "DataElement Update payload -DataType's DataModel must be same as existing dataType datamodel $existingParent.id")

        }

    }
}
