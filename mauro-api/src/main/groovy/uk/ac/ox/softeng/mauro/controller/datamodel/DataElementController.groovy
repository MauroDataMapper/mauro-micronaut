package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
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
        accessControlService.checkRole(Role.EDITOR, dataModel)
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.EDITOR, dataClass)
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
        DataClass dataClass = dataClassRepository.readById(dataClassId)
        accessControlService.checkRole(Role.READER, dataClass)
        ListResponse.from(dataElementRepository.readAllByDataClass_Id(dataClassId))
    }


}
