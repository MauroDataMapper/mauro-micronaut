package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemContentRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TermRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository
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
import reactor.core.publisher.Mono

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataTypes')
class DataTypeController extends AdministeredItemController<DataType, DataModel> {

    DataTypeRepository dataTypeRepository

    DataTypeController(DataTypeRepository dataTypeRepository, DataModelRepository dataModelRepository, AdministeredItemContentRepository<DataType> administeredItemContentRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, administeredItemContentRepository)
        this.dataTypeRepository = dataTypeRepository
    }

    @Get('/{id}')
    Mono<DataType> show(UUID dataTypeId, UUID id) {
        super.show(dataTypeId, id)
    }

    @Post
    Mono<DataType> create(UUID dataModelId, @Body @NonNull DataType dataType) {
        super.create(dataModelId, dataType)
    }

    @Put('/{id}')
    Mono<DataType> update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType) {
        super.update(dataModelId, id, dataType)
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType) {
        super.delete(dataModelId, id, dataType)
    }

    @Get
    Mono<ListResponse<DataType>> list(UUID dataModelId) {
        super.list(dataModelId)
    }

}
