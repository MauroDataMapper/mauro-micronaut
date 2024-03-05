package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataClassCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import jakarta.inject.Inject
import reactor.core.publisher.Mono
import reactor.util.function.Tuple2

import java.util.function.BiFunction

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataClasses')
class DataClassController extends AdministeredItemController<DataClass, DataModel> {

    DataClassCacheableRepository dataClassRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    DataClassController(DataClassCacheableRepository dataClassRepository, DataModelCacheableRepository dataModelRepository, DataModelContentRepository dataModelContentRepository) {
        super(DataClass, dataClassRepository, dataModelRepository, dataModelContentRepository)
        this.dataClassRepository = dataClassRepository
    }

    @Get('/{id}')
    DataClass show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Post
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        super.create(dataModelId, dataClass)
    }

    @Put('/{id}')
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(id, dataClass)
    }

    @Get
    ListResponse<DataClass> list(UUID dataModelId) {
        super.list(dataModelId)
    }

    @Get('/{parentDataClassId}/dataClasses/{id}')
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(id)
    }

    @Post('/{parentDataClassId}/dataClasses')
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass) {

        cleanBody(dataClass)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        DataClass parentDataClass = dataClassRepository.readById(parentDataClassId)
        dataClass.parentDataClass = parentDataClass
        createEntity(dataModel, dataClass)
        return dataClass
    }

    @Put('/{parentDataClassId}/dataClasses/{id}')
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(id, dataClass)
    }

    @Delete('/{parentDataClassId}/dataClasses/{id}')
    HttpStatus delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(id, dataClass)
    }

    @Get('/{parentDataClassId}/dataClasses')
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId) {
        ListResponse.from(dataClassRepository.readAllByParentDataClass_Id(parentDataClassId))

    }




}
