package uk.ac.ox.softeng.mauro.controller.datamodel

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.datamodel.EnumerationValueApi
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
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
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.DataTypeCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository.EnumerationValueCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class EnumerationValueController extends AdministeredItemController<EnumerationValue, DataType> implements EnumerationValueApi {

    @Inject
    DataTypeCacheableRepository dataTypeRepository

    @Inject
    DataModelCacheableRepository dataModelRepository

    EnumerationValueCacheableRepository enumerationValueRepository

    EnumerationValueController(EnumerationValueCacheableRepository enumerationValueRepository, DataTypeCacheableRepository dataTypeRepository, DataModelContentRepository dataModelContentRepository) {
        super(EnumerationValue, enumerationValueRepository, dataTypeRepository, dataModelContentRepository)
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
    @Get(Paths.ENUMERATION_VALUE_LIST)
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId) {
        DataType enumerationType = dataTypeRepository.readById(enumerationTypeId)
        accessControlService.checkRole(Role.EDITOR, enumerationType)
        ListResponse.from(enumerationValueRepository.readAllByEnumerationType_Id(enumerationTypeId))

    }

}
