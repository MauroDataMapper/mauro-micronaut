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
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataElementComponentContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataElementComponentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataElementRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller(Paths.DATA_ELEMENT_COMPONENT_ROUTE)
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataElementComponentController extends AdministeredItemController<DataElementComponent, DataClassComponent> {


    @Inject
    DataElementRepository dataElementRepository

    @Inject
    DataElementComponentRepository dataElementComponentRepository

    @Inject
    DataElementComponentContentRepository dataElementComponentContentRepository


    DataElementComponentController(AdministeredItemCacheableRepository.DataElementComponentCacheableRepository dataElementComponentRepository,
                                   AdministeredItemCacheableRepository.DataClassComponentCacheableRepository dataClassComponentRepository,
                                   DataElementComponentContentRepository dataElementComponentContentRepository) {
        super(DataElementComponent, dataElementComponentRepository, dataClassComponentRepository, dataElementComponentContentRepository)
    }


    @Get(value = Paths.ID_ROUTE)
    DataElementComponent show(@NonNull UUID id) {
        super.show(id)
    }

    @Post
    DataElementComponent create(@NonNull UUID dataClassComponentId, @Body @NonNull DataElementComponent dataElementComponent) {
        super.create(dataClassComponentId, dataElementComponent)
    }

    @Put(value = Paths.ID_ROUTE)
    DataElementComponent update(@NonNull UUID id, @Body @NonNull DataElementComponent dataElementComponent) {
        super.update(id, dataElementComponent)
    }

    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID id, @Body @Nullable DataElementComponent dataElementComponent) {
        super.delete(id, dataElementComponent)
    }

    @Get
    ListResponse<DataElementComponent> list(@NonNull UUID dataClassComponentId) {
        super.list(dataClassComponentId)
    }

    @Put(value = Paths.SOURCE_DATA_ELEMENT_ROUTE)
    DataElementComponent update(@NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.SOURCE, id, dataElementId)
        updated
    }

    @Put(value = Paths.TARGET_DATA_ELEMENT_ROUTE)
    DataElementComponent update(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataElementId) {
        DataElementComponent updated = addDataElement(Type.TARGET, id, dataElementId)
        updated
    }

    @Delete(value = Paths.TARGET_DATA_ELEMENT_ROUTE)
    HttpStatus delete(@NonNull UUID id, @NonNull UUID dataElementId) {
        long deleted = removeDataElement(Type.TARGET, id, dataElementId)
        handleError(HttpStatus.NOT_FOUND,deleted, "Item with id: $id not found")
        return HttpStatus.NO_CONTENT
    }

    @Delete(value = Paths.SOURCE_DATA_ELEMENT_ROUTE)
    HttpStatus delete(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataElementId) {
        long deleted = removeDataElement(Type.SOURCE, id, dataElementId)
        handleError(HttpStatus.NOT_FOUND,deleted, "Item with id: $id not found")
        return HttpStatus.NO_CONTENT
    }

    private DataElementComponent addDataElement(Type type, UUID id, UUID dataElementId) {
        DataElement dataElementToAdd = dataElementRepository.readById(dataElementId)
        handleError(HttpStatus.NOT_FOUND, dataElementToAdd, "Item with id: $dataElementId not found")
        DataElementComponent dataElementComponent = dataElementComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataElementToAdd, "Item with id: $id not found")
        if (type == Type.TARGET) {
            if (dataElementComponent.targetDataElements.id.contains(dataElementToAdd.id)) {
                handleError(HttpStatus.BAD_REQUEST,null, "Item already exists in table DataClassComponentTargetDataClass: $dataElementToAdd.id")
            }
            dataElementComponent.targetDataElements.add(dataElementToAdd)
            dataElementComponentRepository.addTargetDataElement(dataElementComponent.id, dataElementId)

        } else if (type == Type.SOURCE) {
            if (dataElementComponent.sourceDataElements.id.contains(dataElementToAdd.id)) {
                handleError(HttpStatus.BAD_REQUEST,null, "Item already exists in table DataClassComponentSourceDataClass: $dataElementToAdd.id")
            }
            dataElementComponent.sourceDataElements.add(dataElementToAdd)
            dataElementComponentRepository.addSourceDataElement(dataElementComponent.id, dataElementId)
        }
        dataElementComponent
    }

    private Long removeDataElement(Type type, UUID id, UUID dataElementId) {
        DataElement dataElementToRemove = dataElementRepository.readById(dataElementId)
        handleError(HttpStatus.NOT_FOUND,dataElementToRemove, "Item with id: $dataElementId not found")
        DataElementComponent dataElementComponent = dataElementComponentContentRepository.readWithContentById(id)
        handleError(HttpStatus.NOT_FOUND, dataElementComponent, "Item with id: $id not found")
        if (type == Type.TARGET) {
            if (!dataElementComponent.targetDataElements.removeIf(de -> de.id == dataElementId)) {
                handleError(HttpStatus.NOT_FOUND,null, "Item already exists in table DataClassComponentTargetDataElement: $dataElementId")
            }
            return dataElementComponentRepository.removeTargetDataElement(dataElementComponent.id, dataElementId)
        } else if (type == Type.SOURCE) {
            if (!dataElementComponent.sourceDataElements.removeIf(de -> de.id == dataElementId)) {
                handleError(HttpStatus.NOT_FOUND,null, "Item already exists in table DataClassComponentSourceDataElement: $dataElementId")
            }
            return dataElementComponentRepository.removeSourceDataElement(dataElementComponent.id, dataElementId)
        }
    }
}
