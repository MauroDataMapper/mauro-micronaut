package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Slf4j
@Controller('/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements')
@Secured(SecurityRule.IS_ANONYMOUS)
class DataElementController extends AdministeredItemController<DataElement, DataClass> {
    static String MISSING_DATATYPE_ID_MESSAGE_FORMAT = "No dataType id input for update of dataElement: %s"
    static String WRONG_DATAMODEL_FOR_DATA_ELEMENT_DATATYPE = "Attempting to update dataElement datatype with different dataModel"
    DataElementCacheableRepository dataElementRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    @Inject
    DataClassCacheableRepository dataClassRepository

    DataElementController(DataElementCacheableRepository dataElementRepository, DataClassCacheableRepository dataClassRepository, DataModelContentRepository dataModelContentRepository) {
        super(DataElement, dataElementRepository, dataClassRepository, dataModelContentRepository)
        this.dataElementRepository = dataElementRepository
    }

    @Get('/{id}')
    DataElement show(UUID dataModelId, UUID dataClassId, UUID id) {
        super.show(id)
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

    @Put('/{id}')
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement) {
        DataElement cleanItem = super.cleanBody(dataElement) as DataElement
        DataElement existing = administeredItemRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, existing)
        boolean dataTypeHasChanged = isDataTypeChange(existing, dataElement)
        if (dataTypeHasChanged) {
            validateDataTypeInPayload(existing, dataElement)
            existing.dataType.id = dataElement.dataType.id
        }
        boolean hasChanged = updateProperties(existing, cleanItem)
        updateDerivedProperties(existing)
        DataElement updated = existing
        if( hasChanged || dataTypeHasChanged){
            updated = administeredItemRepository.update(existing) as DataElement
        }
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

    private static void validateDataTypeInPayload(DataElement existing, DataElement dataElement) {
        if (!dataElement.dataType.id) {
            log.warn(MISSING_DATATYPE_ID_MESSAGE_FORMAT, dataElement.id)
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, String.format(MISSING_DATATYPE_ID_MESSAGE_FORMAT, dataElement.id))
        }
        if (existing.dataType.dataModel != dataElement.dataType.dataModel) {
            log.error(WRONG_DATAMODEL_FOR_DATA_ELEMENT_DATATYPE, dataElement.dataType.id)
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                    String.format(WRONG_DATAMODEL_FOR_DATA_ELEMENT_DATATYPE, dataElement.dataType.id))
        }
    }

    private static boolean isDataTypeChange(DataElement existing, DataElement dataElement) {
        if (!dataElement.dataType) return false
        return dataElement.dataType?.id != existing.dataType.id
    }
}
