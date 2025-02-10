package uk.ac.ox.softeng.mauro.controller.dataflow

import uk.ac.ox.softeng.mauro.api.dataflow.DataElementComponentApi
import uk.ac.ox.softeng.mauro.ErrorHandler

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataElementComponentContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataElementComponentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataElementRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataElementComponentController extends AdministeredItemController<DataElementComponent, DataClassComponent> implements DataElementComponentApi {

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Inject
    DataElementRepository dataElementRepository

    @Inject
    DataElementComponentContentRepository dataElementComponentContentRepository
    @Inject
    DataFlowRepository dataFlowRepository

    DataElementComponentController(DataElementComponentRepository dataElementComponentRepository,
                                   DataClassComponentRepository dataClassComponentRepository,
                                   DataElementComponentContentRepository dataElementComponentContentRepository) {
        super(DataElementComponent, dataElementComponentRepository, dataClassComponentRepository, dataElementComponentContentRepository)
    }


    @Get(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id) {
       super.show(id)
    }

    @Post(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST)
    DataElementComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @Body @NonNull DataElementComponent dataElementComponent) {
        super.create(dataClassComponentId, dataElementComponent)
    }

    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @NonNull DataElementComponent dataElementComponent) {
        super.update(id, dataElementComponent)
    }

    @Delete(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @Nullable DataElementComponent dataElementComponent) {
        super.delete(id, dataElementComponent)
    }

    @Get(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST)
    ListResponse<DataElementComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId) {
        super.list(dataClassComponentId)
    }

    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    DataElementComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.SOURCE, id, dataElementId, dataClassComponentId)
        updated
    }

    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT)
    DataElementComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.TARGET, id, dataElementId, dataClassComponentId)
        updated
    }

    @Delete(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        removeDataElement(Type.SOURCE, id, dataElementId)
        return HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    @Delete(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT)
    HttpResponse deleteTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        removeDataElement(Type.TARGET, id, dataElementId)
        return HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    private DataElementComponent addDataElement(Type type, UUID id, UUID dataElementId, UUID parentId) {
        DataElement dataElementToAdd = dataElementRepository.readById(dataElementId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementToAdd, "Item with id: $dataElementId not found")
        accessControlService.checkRole(Role.EDITOR, dataElementToAdd)

        DataElementComponent dataElementComponent = dataElementComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementComponent, "Item with id: $id not found")
        accessControlService.checkRole(Role.EDITOR, dataElementComponent)

        switch (type) {
            case Type.TARGET:
                if (dataElementComponent.targetDataElements.id.contains(dataElementToAdd.id)) {
                    ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Item already exists in table DataClassComponentTargetDataClass: $dataElementToAdd.id")
                }
                dataElementComponent.targetDataElements.add(dataElementToAdd)
                dataElementComponentRepository.addTargetDataElement(dataElementComponent.id, dataElementId)
                break
            case Type.SOURCE:
                if (dataElementComponent.sourceDataElements.id.contains(dataElementToAdd.id)) {
                    ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Item already exists in table DataClassComponentSourceDataClass: $dataElementToAdd.id")
                }
                dataElementComponent.sourceDataElements.add(dataElementToAdd)
                dataElementComponentRepository.addSourceDataElement(dataElementComponent.id, dataElementId)
                break
            default:
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "$type Type must be source or target")
        }
        dataElementComponent
    }

    private void removeDataElement(Type type, UUID id, UUID dataElementId) {
        DataElement dataElementToRemove = dataElementRepository.readById(dataElementId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementToRemove,"Item with id: $dataElementId not found")
        accessControlService.checkRole(Role.EDITOR, dataElementToRemove)
        DataElementComponent dataElementComponent = dataElementComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementComponent,"Item with id: $id not found")

        accessControlService.checkRole(Role.EDITOR, dataElementToRemove)
        Long result
        switch (type) {
            case Type.TARGET:
                if (!dataElementComponent.targetDataElements.removeIf(de -> de.id == dataElementId)) {
                    ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item already exists in table DataClassComponentTargetDataElement: $dataElementId")
                }
                result = dataElementComponentRepository.removeTargetDataElement(dataElementComponent.id, dataElementId)
                break
            case Type.SOURCE:
                if (!dataElementComponent.sourceDataElements.removeIf(de -> de.id == dataElementId)) {
                    ErrorHandler.handleError(HttpStatus.NOT_FOUND,  "Item already exists in table DataClassComponentSourceDataElement: $dataElementId")
                }
                result = dataElementComponentRepository.removeSourceDataElement(dataElementComponent.id, dataElementId)
                break
            default:
                handleError(HttpStatus.BAD_REQUEST, type, "Type must be source or target")
                break
        }
        dataElementComponent
    }
}
