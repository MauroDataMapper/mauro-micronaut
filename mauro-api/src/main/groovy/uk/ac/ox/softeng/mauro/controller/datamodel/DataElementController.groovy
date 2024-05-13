package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataElementCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements')
class DataElementController extends AdministeredItemController<DataElement, DataClass> {

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
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        dataElement.dataClass = dataClass
        createEntity(dataClass, dataElement)
        return dataElement

    }

    @Put('/{id}')
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement) {
        super.update(id, dataElement)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID dataClassId, UUID id, @Body @Nullable DataElement dataElement) {
        super.delete(id, dataElement)
    }

    @Get
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId) {
        ListResponse.from(dataElementRepository.readAllByDataClass_Id(dataClassId))
    }


}
