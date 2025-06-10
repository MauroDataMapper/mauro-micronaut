package org.maurodata.controller.dataflow

import org.maurodata.api.dataflow.DataElementComponentApi
import org.maurodata.ErrorHandler
import org.maurodata.audit.Audit
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.facet.EditType

import org.maurodata.web.PaginationParams

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.api.Paths
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.security.Role
import org.maurodata.persistence.dataflow.DataClassComponentRepository
import org.maurodata.persistence.dataflow.DataElementComponentContentRepository
import org.maurodata.persistence.dataflow.DataElementComponentRepository
import org.maurodata.persistence.dataflow.DataFlowRepository
import org.maurodata.persistence.datamodel.DataElementRepository
import org.maurodata.web.ListResponse

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


    @Audit
    @Get(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id) {
        super.show(id)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST)
    DataElementComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @Body @NonNull DataElementComponent dataElementComponent) {
        super.create(dataClassComponentId, dataElementComponent)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @NonNull DataElementComponent dataElementComponent) {
        super.update(id, dataElementComponent)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @Nullable DataElementComponent dataElementComponent) {
        super.delete(id, dataElementComponent)
    }

    @Audit
    @Get(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST_PAGED)
    ListResponse<DataElementComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(dataClassComponentId, params)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    DataElementComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.SOURCE, id, dataElementId, dataClassComponentId)
        updated
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT)
    DataElementComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.TARGET, id, dataElementId, dataClassComponentId)
        updated
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(value = Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId) {
        removeDataElement(Type.SOURCE, id, dataElementId)
        return HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
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
                    ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Item already exists in table DataClassComponentTargetDataClass: $dataElementToAdd.id")
                }
                dataElementComponent.targetDataElements.add(dataElementToAdd)
                dataElementComponentRepository.addTargetDataElement(dataElementComponent.id, dataElementId)
                break
            case Type.SOURCE:
                if (dataElementComponent.sourceDataElements.id.contains(dataElementToAdd.id)) {
                    ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Item already exists in table DataClassComponentSourceDataClass: $dataElementToAdd.id")
                }
                dataElementComponent.sourceDataElements.add(dataElementToAdd)
                dataElementComponentRepository.addSourceDataElement(dataElementComponent.id, dataElementId)
                break
            default:
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "$type Type must be source or target")
        }
        dataElementComponent
    }

    private void removeDataElement(Type type, UUID id, UUID dataElementId) {
        DataElement dataElementToRemove = dataElementRepository.readById(dataElementId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementToRemove, "Item with id: $dataElementId not found")
        accessControlService.checkRole(Role.EDITOR, dataElementToRemove)
        DataElementComponent dataElementComponent = dataElementComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataElementComponent, "Item with id: $id not found")

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
                    ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item already exists in table DataClassComponentSourceDataElement: $dataElementId")
                }
                result = dataElementComponentRepository.removeSourceDataElement(dataElementComponent.id, dataElementId)
                break
            default:
                ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, type, "Type must be source or target")
                break
        }
        dataElementComponent
    }
}
