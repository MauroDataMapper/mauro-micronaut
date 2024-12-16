package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataTypeContentRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.EnumerationValueRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataTypes')
@Secured(SecurityRule.IS_ANONYMOUS)
class DataTypeController extends AdministeredItemController<DataType, DataModel> implements DataTypeApi {

    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    @Inject
    EnumerationValueRepository enumerationValueRepository

    DataTypeController(DataTypeCacheableRepository dataTypeRepository, DataModelCacheableRepository dataModelRepository, DataTypeContentRepository dataTypeContentRepository) {
        super(DataType, dataTypeRepository, dataModelRepository, dataTypeContentRepository)
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
