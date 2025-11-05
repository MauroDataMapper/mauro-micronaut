package org.maurodata.controller.datamodel

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import org.maurodata.api.Paths
import org.maurodata.api.datamodel.EnumerationValueApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository

import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class EnumerationValueController extends AdministeredItemController<EnumerationValue, DataType> implements EnumerationValueApi {

    @Inject
    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    EnumerationValueCacheableRepository enumerationValueRepository

    EnumerationValueController(EnumerationValueCacheableRepository enumerationValueRepository, DataTypeCacheableRepository dataTypeRepository) {
        super(EnumerationValue, enumerationValueRepository, dataTypeRepository)
        this.enumerationValueRepository = enumerationValueRepository
    }

    @Audit
    @Get(Paths.ENUMERATION_VALUE_ID)
    EnumerationValue show(UUID dataModelId, UUID enumerationTypeId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.ENUMERATION_VALUE_LIST)
    EnumerationValue create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue) {
        cleanBody(enumerationValue)
        DataType dataType = dataTypeRepository.readById(enumerationTypeId)
        accessControlService.checkRole(Role.EDITOR, dataType)
        enumerationValue.enumerationType = dataType
        createEntity(dataType, enumerationValue)
        return enumerationValue
    }

    @Audit
    @Put(Paths.ENUMERATION_VALUE_ID)
    EnumerationValue update(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @NonNull EnumerationValue enumerationValue) {
        super.update(id, enumerationValue)
    }

    @Audit(
            parentDomainType = DataType,
            parentIdParamName = 'enumerationTypeId',
            deletedObjectDomainType = EnumerationValue
    )
    @Delete(Paths.ENUMERATION_VALUE_ID)
    HttpResponse delete(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @Nullable EnumerationValue enumerationValue) {
        super.delete(id, enumerationValue)
    }

    @Audit
    @Get(Paths.ENUMERATION_VALUE_LIST_PAGED)
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId, @Nullable PaginationParams params = new PaginationParams()) {
        
        DataType enumerationType = dataTypeRepository.readById(enumerationTypeId)
        accessControlService.checkRole(Role.EDITOR, enumerationType)
        ListResponse.from(enumerationValueRepository.readAllByEnumerationType_Id(enumerationTypeId), params)
    }

}
