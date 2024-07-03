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
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataClassComponentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller(Paths.DATA_CLASS_COMPONENTS_ROUTE)
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataClassComponentController extends AdministeredItemController<DataClassComponent, DataFlow> {


    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassRepository

    @Inject
    DataClassComponentRepository dataClassComponentRepository

    @Inject
    DataClassComponentContentRepository dataClassComponentContentRepository

    DataClassComponentController(AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentCacheableRepository,
                                 AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository,
                                 DataClassComponentContentRepository dataClassComponentContentRepository) {
        super(DataClassComponent, dataClassComponentCacheableRepository, dataFlowRepository, dataClassComponentContentRepository)
    }


    @Get(value = Paths.ID_ROUTE)
    DataClassComponent show(UUID id) {
        super.show(id)
    }

    @Post
    DataClassComponent create(@NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent) {
        super.create(dataFlowId, dataClassComponent)
    }

    @Put(value = Paths.ID_ROUTE)
    DataClassComponent update(@NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent) {
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
    DataClassComponent update(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.SOURCE, id, dataClassId)
        updated
    }

    @Put(value = Paths.TARGET_DATA_CLASS_ROUTE)
    DataClassComponent update(@NonNull UUID id, @NonNull UUID dataClassId) {
        DataClassComponent updated = addDataClass(Type.TARGET, id, dataClassId)
        updated
    }

    @Delete(value = Paths.TARGET_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID id, @NonNull UUID dataClassId) {
        long deleted = removeDataClass(Type.TARGET, id, dataClassId)
        handleError(HttpStatus.NOT_FOUND, deleted, "Item with id: $id not found")
        return HttpStatus.NO_CONTENT
    }

    @Delete(value = Paths.SOURCE_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) {
        long deleted = removeDataClass(Type.SOURCE, id, dataClassId)
        handleError(HttpStatus.NOT_FOUND,deleted, "Item with id: $id not found, type: $Type.SOURCE")
        return HttpStatus.NO_CONTENT
    }

    private DataClassComponent addDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToAdd = dataClassRepository.readById(dataClassId)
        handleError(HttpStatus.NOT_FOUND, dataClassToAdd, "Item with id: $id not found")
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataClassComponent, "Item with id: $id not found")
        if (type == Type.TARGET) {
            if (dataClassComponent.targetDataClasses.id.contains(dataClassToAdd.id)) {
                handleError(HttpStatus.BAD_REQUEST,null, "Item already exists in table DataClassComponentTargetDataClass: $dataClassToAdd.id")
            }
            dataClassComponent.targetDataClasses.add(dataClassToAdd)
            dataClassComponentRepository.addTargetDataClass(dataClassComponent.id, dataClassId)
        } else if (type == Type.SOURCE) {
            if (dataClassComponent.sourceDataClasses.id.contains(dataClassToAdd.id)) {
                handleError(HttpStatus.BAD_REQUEST, null, "Item already exists in table DataClassComponentSourceDataClass: $dataClassToAdd.id");
            }
            dataClassComponent.sourceDataClasses.add(dataClassToAdd)
            dataClassComponentRepository.addSourceDataClass(dataClassComponent.id, dataClassId)
        }
        dataClassComponent
    }

    private Long removeDataClass(Type type, UUID id, UUID dataClassId) {
        DataClass dataClassToRemove = dataClassRepository.readById(dataClassId)
        handleError(HttpStatus.NOT_FOUND, dataClassToRemove, "Item with id: $dataClassId not found")
        DataClassComponent dataClassComponent = dataClassComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataClassComponent, "Item with id: $id not found")
        if (type == Type.TARGET) {
            if (!dataClassComponent.targetDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                handleError(HttpStatus.NOT_FOUND,null, "Item does not exist in table DataClassComponentTargetDataClass: $dataClassId")
            }
            dataClassComponent.targetDataClasses.remove(dataClassToRemove)
            return dataClassComponentRepository.removeTargetDataClass(dataClassComponent.id, dataClassId)
        } else if (type == Type.SOURCE) {
            if (!dataClassComponent.sourceDataClasses.removeIf(dc -> dc.id == dataClassId)) {
                handleError(HttpStatus.NOT_FOUND,null, "Item does not exist in table DataClassComponentSourceDataClass: $dataClassId")
            }
            dataClassComponent.sourceDataClasses.remove(dataClassToRemove)
            return dataClassComponentRepository.removeSourceDataClass(dataClassComponent.id, dataClassId)
        }
    }
}
