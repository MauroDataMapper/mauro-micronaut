package org.maurodata.controller.dataflow

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
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.dataflow.DataClassComponentApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.dataflow.DataClassComponentContentRepository
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller()
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataClassComponentController extends AdministeredItemController<DataClassComponent, DataFlow> implements DataClassComponentApi {
  @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentRepository

    DataClassComponentContentRepository dataClassComponentContentRepository

    AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository

    @Inject
    DataClassComponentController(AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentRepository,
                                 AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository,
                                 DataClassComponentContentRepository dataClassComponentContentRepository) {
        super(DataClassComponent, dataClassComponentRepository, dataFlowRepository, dataClassComponentContentRepository)
        this.dataClassComponentRepository = dataClassComponentRepository
        this.dataClassComponentContentRepository = dataClassComponentContentRepository
        this.dataFlowRepository = dataFlowRepository
        this.dataClassRepository = dataClassRepository
    }

    @Audit
    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id) {
        super.show(id)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    DataClassComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent) {
        super.create(dataFlowId, dataClassComponent)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent) {
        super.update(id, dataClassComponent)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent) {
        super.delete(id, dataClassComponent)
    }

    @Audit
    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_LIST_PAGED)
    ListResponse<DataClassComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Nullable PaginationParams params = new PaginationParams()) {
        
        super.list(dataFlowId, params)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    DataClassComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.SOURCE, id, dataClassId)
        updated
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    DataClassComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.TARGET, id, dataClassId)
        updated
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        removeDataClass(Type.SOURCE, id, dataClassId)
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    HttpResponse deleteTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        removeDataClass(Type.TARGET, id, dataClassId)
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

    private DataClassComponent addDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToAdd = dataClassRepository.readById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToAdd, "item not found : $id")
        accessControlService.checkRole(Role.EDITOR, dataClassToAdd)
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassComponent, "item not found : $id")
        accessControlService.checkRole(Role.EDITOR, dataClassComponent)

        switch (type) {
            case Type.TARGET:
                if (dataClassComponent.targetDataClasses.id.contains(dataClassToAdd.id)) {
                    ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Item already exists in table DataClassComponentTargetDataClass: : $dataClassToAdd.id")
                }
                dataClassComponent.targetDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addTargetDataClass(dataClassComponent.id, dataClassId)
                break
            case Type.SOURCE:
                if (dataClassComponent.sourceDataClasses.id.contains(dataClassToAdd.id)) {
                    ErrorHandler.handleErrorOnNullObject(HttpStatus.UNPROCESSABLE_ENTITY, null, "Item already exists in table DataClassComponentSourceDataClass: $dataClassToAdd.id")
                }
                dataClassComponent.sourceDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addSourceDataClass(dataClassComponent.id, dataClassId)
                break
            default:
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "$type Type must be source or target")
        }
        dataClassComponent
    }

    private DataClassComponent removeDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToRemove = dataClassRepository.readById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToRemove, "Item with id: $dataClassId not found")
        accessControlService.checkRole(Role.EDITOR, dataClassToRemove)
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToRemove, "Item with id: $id not found")
        accessControlService.checkRole(Role.EDITOR, dataClassComponent)

        Long result
        switch (type) {
            case Type.TARGET:
                if (!dataClassComponent.targetDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                    ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item does not exist in table DataClassComponentTargetDataClass: $dataClassId")
                }
                result = dataClassComponentRepository.removeTargetDataClass(dataClassComponent.id, dataClassId)
                break
            case Type.SOURCE:
                if (!dataClassComponent.sourceDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                    ErrorHandler.handleError(HttpStatus.NOT_FOUND, "Item does not exist in table DataClassComponentSourceDataClass: $dataClassId")
                }
                result = dataClassComponentRepository.removeSourceDataClass(dataClassComponent.id, dataClassId)
                break
            default:
                ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Type must be source or target")
                break;
        }
        dataClassComponent
    }
}
