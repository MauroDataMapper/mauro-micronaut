package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.terminology.Term
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.exception.MauroInternalException
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository
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
import jakarta.inject.Inject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataTypes')
class DataTypeController extends AdministeredItemController<DataType, DataModel> {

    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    DataTypeController(DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository, AdministeredItemContentRepository<DataType> administeredItemContentRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, administeredItemContentRepository)
        this.dataTypeRepository = dataTypeRepository
    }

    @Get('/{id}')
    DataType show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Post
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType) {
        super.create(dataModelId, dataType)
        if(dataType.enumerationValues) {
            dataType.enumerationValues.each {enumValue ->
                enumValue.enumerationType = (DataType) dataType
                enumerationValueRepository.save(enumValue)
            }
        }
        return dataType
    }

    @Put('/{id}')
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType) {
        super.update(id, dataType)
    }

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType) {
        super.delete(id, dataType)
    }

    @Get
    ListResponse<DataType> list(UUID dataModelId) {
        super.list(dataModelId)
    }

}
