package uk.ac.ox.softeng.mauro.controller.dataflow

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.*
import io.micronaut.http.exceptions.HttpStatusException
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.validation.constraints.NotNull
import uk.ac.ox.softeng.mauro.controller.model.AdministeredItemController
import uk.ac.ox.softeng.mauro.controller.terminology.Paths
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.AdministeredItemCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowContentRepository
import uk.ac.ox.softeng.mauro.persistence.dataflow.DataFlowRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@CompileStatic
@Controller(Paths.DATA_FLOW_ROUTE)
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataFlowController extends AdministeredItemController<DataFlow, DataModel> {
    @Inject
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository

    @Inject
    DataFlowRepository dataFlowRepository


    DataFlowController(AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowCacheableRepository,
                       ModelCacheableRepository.DataModelCacheableRepository dataModelRepository, DataFlowContentRepository dataFlowContentRepository) {
        super(DataFlow, dataFlowCacheableRepository, dataModelRepository, dataFlowContentRepository)
    }


    @Get(value = Paths.ID_ROUTE)
    DataFlow show(@NonNull UUID dataModelId, @NonNull UUID id) {
        super.show(id)
    }

    @Post
    DataFlow create(@NonNull UUID dataModelId, @Body @NonNull DataFlow dataFlow) {
        DataModel target = dataModelRepository.readById(dataModelId)
        handleError(HttpStatus.NOT_FOUND, target, "Item with id: $dataModelId not found")
        accessControlService.checkRole(Role.EDITOR, target)
        DataModel retrievedSource = dataModelRepository.readById(dataFlow.source.id)
        handleError(HttpStatus.NOT_FOUND, retrievedSource, "Item with id: $dataFlow.source.id not found")
        dataFlow.source = retrievedSource
        accessControlService.checkRole(Role.EDITOR, target)
        dataFlow.target = target
        createEntity(target, dataFlow)
    }

    @Put(value = Paths.ID_ROUTE)
    DataFlow update(@NonNull UUID id, @Body @NonNull DataFlow dataFlow) {
        super.update(id, dataFlow)
    }


    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID dataModelId, @NonNull UUID id, @Body @Nullable DataFlow dataFlow) {
        DataFlow retrieved = dataFlowRepository.readById(id)
        handleError(HttpStatus.NOT_FOUND, retrieved, "Item with id: $id not found")
        if (retrieved.target.id != dataModelId) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "$dataModelId is not target dataModel for dataFlow $id")
        }
        super.delete(id, dataFlow)
    }

    @Get
    ListResponse<DataFlow> list(@NotNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type) {
        if (!type || type == Type.TARGET) {
            return super.list(dataModelId)
        }
        DataModel source = dataModelRepository.findById(dataModelId)
        handleError(HttpStatus.NOT_FOUND, source, "Item with id: $dataModelId not found")
        ListResponse.from(dataFlowRepository.findAllBySource(source))
    }
}
