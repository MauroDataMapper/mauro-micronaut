package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
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
import reactor.core.publisher.Mono

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues')
class EnumerationValueController extends AdministeredItemController<EnumerationValue, DataType> {

    @Inject
    DataTypeRepository dataTypeRepository

    EnumerationValueRepository enumerationValueRepository

    EnumerationValueController(EnumerationValueRepository enumerationValueRepository, DataTypeRepository dataTypeRepository, AdministeredItemContentRepository<EnumerationValue> administeredItemContentRepository) {
        super(EnumerationValue, enumerationValueRepository, dataTypeRepository, administeredItemContentRepository)
        this.enumerationValueRepository = enumerationValueRepository
    }

    @Get('/{id}')
    Mono<EnumerationValue> show(UUID dataModelId, UUID enumerationTypeId, UUID id) {
        super.show(enumerationTypeId, id)
    }

    @Post
    Mono<EnumerationValue> create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue) {
        return super.create(enumerationTypeId, enumerationValue)
    }

    @Put('/{id}')
    Mono<EnumerationValue> update(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @NonNull EnumerationValue enumerationValue) {
        super.update(enumerationTypeId, id, enumerationValue)
    }

    @Delete('/{id}')
    Mono<HttpStatus> delete(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @Nullable EnumerationValue enumerationValue) {
        super.delete(enumerationTypeId, id, enumerationValue)
    }

    @Get
    Mono<ListResponse<EnumerationValue>> list(UUID dataModelId, UUID enumerationTypeId) {
        super.list(enumerationTypeId)
    }

}
