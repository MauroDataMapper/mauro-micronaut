package uk.ac.ox.softeng.mauro.controller.dataflow

import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.dataflow.DataFlowApi

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.constraints.NotNull
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller(Paths.DATA_FLOW_ROUTE)
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataFlowController extends AdministeredItemController<DataFlow, DataModel> implements DataFlowApi {

    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository

    @Inject
    DataFlowRepository dataFlowRepository


    DataFlowController(DataFlowRepository dataFlowRepository, ModelCacheableRepository.DataModelCacheableRepository dataModelRepository, DataFlowContentRepository dataFlowContentRepository) {
        super(DataFlow, dataFlowRepository, dataModelRepository, dataFlowContentRepository)
    }


    @Get(value = Paths.ID_ROUTE)
    DataFlow show(@NonNull UUID id) {
        super.show(id)
    }

    @Post
    DataFlow create(@NonNull UUID dataModelId, @Body @NonNull DataFlow dataFlow) {
        DataModel source = dataModelRepository.findById(dataFlow.source.id)
        accessControlService.checkRole(Role.READER, source)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, source, "Datamodel not found : $dataFlow.source.id")
        DataFlow created = super.create(dataModelId, dataFlow)
        show(created.id)
    }

    @Put(value = Paths.ID_ROUTE)
    DataFlow update(@NonNull UUID id, @Body @NonNull DataFlow dataFlow) {
        super.update(id, dataFlow)
    }


    @Delete(value = Paths.ID_ROUTE)
    @Transactional
    HttpStatus delete(@NonNull UUID dataModelId, @NonNull UUID id, @Body @Nullable DataFlow dataFlow) {
        super.delete(id, dataFlow)
    }

    @Get
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
