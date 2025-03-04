package uk.ac.ox.softeng.mauro.controller.dataflow

import uk.ac.ox.softeng.mauro.api.dataflow.DataClassComponentApi
import uk.ac.ox.softeng.mauro.ErrorHandler

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
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
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller()
@Slf4j
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataClassComponentController extends AdministeredItemController<DataClassComponent, DataFlow> implements DataClassComponentApi {

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository

    @Inject
    DataFlowRepository dataFlowRepository

    DataClassComponentController(DataClassComponentRepository dataClassComponentRepository,
                                 DataFlowRepository dataFlowRepository,
                                 DataClassComponentContentRepository dataClassComponentContentRepository) {
        super(DataClassComponent, dataClassComponentRepository, dataFlowRepository, dataClassComponentContentRepository)
    }


    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id) {
        super.show(id)
    }

    @Post(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    DataClassComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent) {
        super.create(dataFlowId, dataClassComponent)
    }

    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent) {
        super.update(id, dataClassComponent)
    }

    @Delete(Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent) {
        super.delete(id, dataClassComponent)
    }

    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    ListResponse<DataClassComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId) {
        super.list(dataFlowId)
    }

    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    DataClassComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.SOURCE, id, dataClassId)
        updated
    }

    @Put(Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    DataClassComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.TARGET, id, dataClassId)
        updated
    }

    @Delete(Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        removeDataClass(Type.SOURCE, id, dataClassId)
        HttpResponse.status(HttpStatus.NO_CONTENT)
    }

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
                    ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Item already exists in table DataClassComponentTargetDataClass: : $dataClassToAdd.id")
                }
                dataClassComponent.targetDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addTargetDataClass(dataClassComponent.id, dataClassId)
                break
            case Type.SOURCE:
                if (dataClassComponent.sourceDataClasses.id.contains(dataClassToAdd.id)) {
                    ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, null, "Item already exists in table DataClassComponentSourceDataClass: $dataClassToAdd.id")
                }
                dataClassComponent.sourceDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addSourceDataClass(dataClassComponent.id, dataClassId)
                break
            default:
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "$type Type must be source or target")
        }
        dataClassComponent
    }

    private DataClassComponent removeDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToRemove = dataClassRepository.readById(dataClassId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToRemove,"Item with id: $dataClassId not found")
        accessControlService.checkRole(Role.EDITOR, dataClassToRemove)
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, dataClassToRemove,"Item with id: $id not found")
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
                ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "Type must be source or target")
                break;
        }
        dataClassComponent
    }
}
