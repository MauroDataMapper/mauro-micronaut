package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.facet.EditType

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
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
@Controller
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

    @Audit
    @Get(Paths.DATA_TYPE_ID)
    DataType show(UUID dataModelId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.DATA_TYPE_LIST)
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

    @Audit
    @Put(Paths.DATA_TYPE_ID)
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType) {
        super.update(id, dataType)
    }

    @Audit(
        parentDomainType = DataModel,
        parentIdParamName = 'dataModelId',
        deletedObjectDomainType = DataType
    )
    @Delete(Paths.DATA_TYPE_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType) {
        super.delete(id, dataType)
    }

    @Audit
    @Get(Paths.DATA_TYPE_LIST)
    ListResponse<DataType> list(UUID dataModelId) {
        super.list(dataModelId)
    }

}
