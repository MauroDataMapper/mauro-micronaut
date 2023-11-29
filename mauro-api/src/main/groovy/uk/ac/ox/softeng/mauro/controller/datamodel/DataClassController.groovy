package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationshipType
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
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

    DataClassRepository dataClassRepository

    @Inject
    DataModelRepository dataModelRepository

    DataClassController(DataClassRepository dataClassRepository, DataModelRepository dataModelRepository, AdministeredItemContentRepository<DataClass> administeredItemContentRepository) {
        super(DataClass, dataClassRepository, dataModelRepository, administeredItemContentRepository)
        this.dataClassRepository = dataClassRepository
    }

    @Get('/{id}')
    Mono<DataClass> show(UUID dataModelId, UUID id) {
        super.show(dataModelId, id)
    }

    @Post
    Mono<DataClass> create(UUID dataModelId, @Body @NonNull DataClass dataClass) {
        return super.create(dataModelId, dataClass)
    }

    @Put('/{id}')
    Mono<DataClass> update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(dataModelId, id, dataClass)
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(dataModelId, id, dataClass)
    }

    @Get
    Mono<ListResponse<DataClass>> list(UUID dataModelId) {
        super.list(dataModelId)
    }

    @Get('/{parentDataClassId}/dataClasses/{id}')
    Mono<DataClass> show(UUID dataModelId, UUID parentDataClassId, UUID id) {
        super.show(dataModelId, id)
    }

    @Post('/{parentDataClassId}/dataClasses')
    Mono<DataClass> create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass) {

        cleanBody(dataClass)
        Mono.zip(dataModelRepository.readById(dataModelId),
            dataClassRepository.readByDataModelIdAndId(dataModelId, parentDataClassId)).flatMap {
            Tuple2 <DataModel, DataClass> tuple2 ->
                DataModel dataModel = tuple2.getT1()
                dataClass.parentDataClass = tuple2.getT2()
                createEntity(dataModel, dataClass)
        }


    }

    @Put('/{parentDataClassId}/dataClasses/{id}')
    Mono<DataClass> update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass) {
        super.update(dataModelId, id, dataClass)
    }

    @Delete('/{parentDataClassId}/dataClasses/{id}')
    Mono<HttpStatus> delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass) {
        super.delete(dataModelId, id, dataClass)
    }

    @Get('/{parentDataClassId}/dataClasses')
    Mono<ListResponse<DataClass>> list(UUID dataModelId, UUID parentDataClassId) {
        dataClassRepository.readAllByDataModelIdAndParentDataClassId(dataModelId, parentDataClassId).flatMap {DataClass item ->
            Mono.zip(Mono.just(item), pathRepository.readParentItems(item), (BiFunction<DataClass, List<AdministeredItem>, DataClass>) {it, _ -> it})
        }.collectList().map {List<DataClass> items ->
            items.each {((AdministeredItem) it).updatePath()}
            ListResponse.from(items)
        }

    }




}
