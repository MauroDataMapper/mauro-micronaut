package uk.ac.ox.softeng.mauro.controller.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.controller.terminology.Paths
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
@Controller(Paths.DATA_CLASS_COMPONENTS_ROUTE)
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataClassComponentController extends AdministeredItemController<DataClassComponent, DataFlow> {

    @Inject
    AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository
    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository

    @Inject
    DataFlowRepository dataFlowRepository

    DataClassComponentController(AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository,
                                 AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository,
                                 DataClassComponentContentRepository dataClassComponentContentRepository) {
        super(DataClassComponent, dataClassComponentCacheableRepository, dataFlowRepository, dataClassComponentContentRepository)
    }


    @Get(value = Paths.ID_ROUTE)
    DataClassComponent show(@NonNull UUID dataFlowId, @NonNull UUID id) {
        DataFlow parent = dataFlowRepository.findById(dataFlowId)
        DataClassComponent retrieved = super.show(id)
        retrieved.dataFlow = parent
        retrieved
    }

    @Post
    DataClassComponent create(@NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent) {
        super.create(dataFlowId, dataClassComponent)
    }

    @Put(value = Paths.ID_ROUTE)
    DataClassComponent update(@NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent) {
        DataClassComponent retrieved = dataClassComponentRepository.readById(id)
        accessControlService.checkRole(Role.EDITOR, retrieved)
        super.update(id, dataClassComponent)
    }

    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent) {
        super.delete(id, dataClassComponent)
    }

    @Get
    ListResponse<DataClassComponent> list(@NonNull UUID dataFlowId) {
        super.list(dataFlowId)
    }

    @Put(value = Paths.SOURCE_DATA_CLASS_ROUTE)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.SOURCE, id, dataClassId, dataFlowId)
        updated
    }

    @Put(value = Paths.TARGET_DATA_CLASS_ROUTE)
    DataClassComponent update(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.TARGET, id, dataClassId, dataFlowId)
        updated
    }

    @Delete(value = Paths.TARGET_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID id, @NonNull UUID dataClassId) {
        removeDataClass(Type.TARGET, id, dataClassId)
        return HttpStatus.NO_CONTENT
    }

    @Delete(value = Paths.SOURCE_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        removeDataClass(Type.SOURCE, id, dataClassId)
        return HttpStatus.NO_CONTENT
    }

    private DataClassComponent addDataClass(Type type, UUID id, UUID dataClassId, UUID parentId) {
        DataClass dataClassToAdd = dataClassRepository.readById(dataClassId)
        handleError(HttpStatus.NOT_FOUND, dataClassToAdd, "Item with id: $id not found")
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataClassComponent, "Item with id: $id not found")

        DataFlow parent = dataFlowRepository.readById(parentId)
        accessControlService.checkRole(Role.EDITOR, parent)
        switch (type) {
            case Type.TARGET:
                if (dataClassComponent.targetDataClasses.id.contains(dataClassToAdd.id)) {
                    handleError(HttpStatus.BAD_REQUEST, null, "Item already exists in table DataClassComponentTargetDataClass: $dataClassToAdd.id")
                }
                dataClassComponent.targetDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addTargetDataClass(dataClassComponent.id, dataClassId)
                break;
            case Type.SOURCE:
                if (dataClassComponent.sourceDataClasses.id.contains(dataClassToAdd.id)) {
                    handleError(HttpStatus.BAD_REQUEST, null, "Item already exists in table DataClassComponentSourceDataClass: $dataClassToAdd.id");
                }
                dataClassComponent.sourceDataClasses.add(dataClassToAdd)
                dataClassComponentRepository.addSourceDataClass(dataClassComponent.id, dataClassId)
                break;
            default:
                handleError(HttpStatus.BAD_REQUEST, type, "Type must be source or target")
        }
        dataClassComponentCacheableRepository.invalidate(dataClassComponent)
        dataClassComponent
    }

    private DataClassComponent removeDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToRemove = dataClassRepository.readById(dataClassId)
        handleError(HttpStatus.NOT_FOUND, dataClassToRemove, "Item with id: $dataClassId not found")
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataClassComponent, "Item with id: $id not found")

        accessControlService.checkRole(Role.EDITOR, dataClassToRemove)
        Long result
        switch (type) {
            case Type.TARGET:
                if (!dataClassComponent.targetDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                    handleError(HttpStatus.NOT_FOUND, null, "Item does not exist in table DataClassComponentTargetDataClass: $dataClassId")
                }
                result = dataClassComponentRepository.removeTargetDataClass(dataClassComponent.id, dataClassId)
                break;
            case Type.SOURCE:
                if (!dataClassComponent.sourceDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                    handleError(HttpStatus.NOT_FOUND, null, "Item does not exist in table DataClassComponentSourceDataClass: $dataClassId")
                }
                result = dataClassComponentRepository.removeSourceDataClass(dataClassComponent.id, dataClassId)
                break;
            default:
                handleError(HttpStatus.BAD_REQUEST, type, "Type must be source or target")

        }
        handleError(HttpStatus.NOT_FOUND, result, "data class not found ,$dataClassId")
        dataClassComponentCacheableRepository.invalidate(dataClassComponent)
        dataClassComponent
    }
}
