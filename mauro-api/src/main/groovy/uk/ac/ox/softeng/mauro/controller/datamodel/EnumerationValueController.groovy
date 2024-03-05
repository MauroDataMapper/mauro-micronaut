package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueContentRepository
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
class EnumerationValueController extends AdministeredItemController<EnumerationValue, DataModel> {

    @Inject
    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    EnumerationValueCacheableRepository enumerationValueRepository

    EnumerationValueController(EnumerationValueCacheableRepository enumerationValueRepository, DataModelCacheableRepository dataModelRepository, EnumerationValueContentRepository enumerationValueContentRepository) {
        super(EnumerationValue, enumerationValueRepository, dataModelRepository, enumerationValueContentRepository)
        this.enumerationValueRepository = enumerationValueRepository
    }

    @Get('/{id}')
    EnumerationValue show(UUID dataModelId, UUID enumerationTypeId, UUID id) {
        super.show(id)
    }

    @Post
    EnumerationValue create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue) {

        cleanBody(enumerationValue)
        DataModel dataModel = dataModelRepository.readById(dataModelId)
        DataType dataType = dataTypeRepository.readById(enumerationTypeId)
        enumerationValue.enumerationType = dataType
        createEntity(dataModel, enumerationValue)
        return enumerationValue
    }

    @Put('/{id}')
    EnumerationValue update(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @NonNull EnumerationValue enumerationValue) {
        super.update(id, enumerationValue)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @Nullable EnumerationValue enumerationValue) {
        super.delete(id, enumerationValue)
    }

    @Get
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId) {
        ListResponse.from(enumerationValueRepository.readAllByEnumerationType_Id(enumerationTypeId))

    }

}
