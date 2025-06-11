package org.maurodata.controller.dataflow

import org.maurodata.ErrorHandler
import org.maurodata.api.dataflow.DataFlowApi
import org.maurodata.audit.Audit
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.facet.EditType

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.constraints.NotNull
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.api.Paths
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.dataflow.DataFlowContentRepository
import org.maurodata.persistence.dataflow.DataFlowRepository
import org.maurodata.web.ListResponse

@CompileStatic
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataFlowController extends AdministeredItemController<DataFlow, DataModel> implements DataFlowApi {

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository

    @Inject
    DataFlowRepository dataFlowRepository


    DataFlowController(DataFlowRepository dataFlowRepository, ModelCacheableRepository.DataModelCacheableRepository dataModelRepository, DataFlowContentRepository dataFlowContentRepository) {
        super(DataFlow, dataFlowRepository, dataModelRepository, dataFlowContentRepository)
    }


    @Audit
    @Get(Paths.DATA_FLOW_ID)
    DataFlow show(@NonNull UUID dataModelId, @NonNull UUID id) {
        super.show(id)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Post(Paths.DATA_FLOW_LIST)
    DataFlow create(@NonNull UUID dataModelId, @Body @NonNull DataFlow dataFlow) {
        DataModel source = dataModelRepository.findById(dataFlow.source.id)
        accessControlService.checkRole(Role.READER, source)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, source, "Datamodel not found : $dataFlow.source.id")
        DataFlow created = super.create(dataModelId, dataFlow)
        show(created.id)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Put(Paths.DATA_FLOW_ID)
    DataFlow update(@NonNull UUID dataModelId, @NonNull UUID id, @Body @NonNull DataFlow dataFlow) {
        super.update(id, dataFlow)
    }

    @Audit(level = Audit.AuditLevel.FILE_ONLY)
    @Delete(Paths.DATA_FLOW_ID)
    @Transactional
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID id, @Body @Nullable DataFlow dataFlow) {
        super.delete(id, dataFlow)
    }

    @Audit
    @Get(Paths.DATA_FLOW_LIST)
    ListResponse<DataFlow> list(@NotNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type) {
        if (!type || type == Type.TARGET) {
            return super.list(dataModelId)
        }
        DataModel source = dataModelRepository.findById(dataModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, source, "Item with id: $dataModelId not found")
        List<DataFlow> sourceDataFlowList = dataFlowRepository.findAllBySource(source)
        ListResponse.from(sourceDataFlowList.findAll{accessControlService.canDoRole(Role.READER, it)})
    }

}
