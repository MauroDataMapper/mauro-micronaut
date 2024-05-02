package uk.ac.ox.softeng.mauro.controller.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller('/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues')
class EnumerationValueController extends AdministeredItemController<EnumerationValue, DataType> {

    @Inject
    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    EnumerationValueCacheableRepository enumerationValueRepository

    EnumerationValueController(EnumerationValueCacheableRepository enumerationValueRepository, DataTypeCacheableRepository dataTypeRepository, DataModelContentRepository dataModelContentRepository) {
        super(EnumerationValue, enumerationValueRepository, dataTypeRepository, dataModelContentRepository)
        this.enumerationValueRepository = enumerationValueRepository
    }

    @Get('/{id}')
    EnumerationValue show(UUID dataModelId, UUID enumerationTypeId, UUID id) {
        super.show(id)
    }

    @Post
    EnumerationValue create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue) {
        cleanBody(enumerationValue)
        DataType dataType = dataTypeRepository.readById(enumerationTypeId)
        accessControlService.checkRole(Role.EDITOR, dataType)
        enumerationValue.enumerationType = dataType
        createEntity(dataType, enumerationValue)
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
        DataType enumerationType = dataTypeRepository.readById(enumerationTypeId)
        accessControlService.checkRole(Role.EDITOR, enumerationType)
        ListResponse.from(enumerationValueRepository.readAllByEnumerationType_Id(enumerationTypeId))

    }

}
