package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataClassRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataClasses')
class DataClassController extends AdministeredItemController<DataClass, DataModel> {

    DataClassRepository dataClassRepository

    DataClassController(DataClassRepository dataClassRepository, DataModelRepository dataModelRepository, AdministeredItemContentRepository<DataClass> administeredItemContentRepository) {
        super(DataClass, dataClassRepository, dataModelRepository, administeredItemContentRepository)
        this.dataClassRepository = dataClassRepository
    }

    @Get('/{id}')
    Mono<DataClass> show(UUID dataClassId, UUID id) {
        super.show(dataClassId, id)
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

}
