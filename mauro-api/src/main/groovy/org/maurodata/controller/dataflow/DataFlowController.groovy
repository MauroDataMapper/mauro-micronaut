package org.maurodata.controller.dataflow

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.constraints.NotNull
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.dataflow.DataFlowApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemController
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.ModelItem
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.dataflow.DataFlowContentRepository
import org.maurodata.plugin.exporter.DataFlowExporterPlugin
import org.maurodata.plugin.exporter.ModelItemExporterPlugin
import org.maurodata.plugin.importer.DataFlowImporterPlugin
import org.maurodata.plugin.importer.json.JsonDataFlowImporterPlugin
import org.maurodata.service.dataflow.DataflowService
import org.maurodata.util.exporter.ExporterUtils
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
class DataFlowController extends AdministeredItemController<DataFlow, DataModel> implements DataFlowApi {

    AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository
    ModelCacheableRepository.DataModelCacheableRepository dataModelRepository
    DataFlowContentRepository dataFlowContentRepository
    DataflowService dataFlowService


    DataFlowController(AdministeredItemCacheableRepository.DataFlowCacheableRepository dataFlowRepository,
                       ModelCacheableRepository.DataModelCacheableRepository dataModelRepository,
                       DataFlowContentRepository dataFlowContentRepository, DataflowService dataFlowService) {
        super(DataFlow, dataFlowRepository, dataModelRepository, dataFlowContentRepository)
        this.dataFlowRepository = dataFlowRepository
        this.dataModelRepository = dataModelRepository
        this.dataFlowContentRepository = dataFlowContentRepository
        this.dataFlowService = dataFlowService
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
        DataFlow created = super.create(dataModelId, dataFlow) as DataFlow
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
    @Get(Paths.DATA_FLOW_LIST_PAGED)
    ListResponse<DataFlow> list(@NotNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type, @Nullable PaginationParams params = new PaginationParams()) {

        if (!type || type == Type.TARGET) {
            return super.list(dataModelId)
        }
        DataModel source = dataModelRepository.findById(dataModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, source, "Item with id: $dataModelId not found")
        List<DataFlow> sourceDataFlowList = dataFlowRepository.findAllBySource(source)
        ListResponse.from(sourceDataFlowList.findAll {accessControlService.canDoRole(Role.READER, it)}, params)
    }

    @Get(Paths.DATA_FLOW_EXPORTERS)
    List<DataFlowExporterPlugin> dataFlowExporters() {
        dataFlowService.getMauroPluginService().listPlugins(DataFlowExporterPlugin)
    }

    @Get(Paths.DATA_FLOW_IMPORTERS)
    List<DataFlowImporterPlugin> dataFlowImporters() {
        dataFlowService.getMauroPluginService().listPlugins(DataFlowImporterPlugin)
    }

    @Audit
    @Get(Paths.DATA_FLOW_EXPORT)
    HttpResponse<byte[]> exportModel(@NonNull UUID dataModelId, @NonNull UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        ModelItemExporterPlugin mauroPlugin = dataFlowService.getModelItemExporterPlugin(namespace, name, version)
        DataFlow existing = dataFlowContentRepository.readWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, existing,"dataFlow Id ${id} not found")
        accessControlService.checkRole(Role.READER, existing)
        DataModel parent = dataModelRepository.findById(dataModelId)
        accessControlService.checkRole(Role.READER, parent)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, parent,"dataModel Id ${dataModelId} not found")
        if (parent.id != existing.target.id) {
            ErrorHandler.handleError(HttpStatus.BAD_REQUEST, "DataModel with id $dataModelId is not parent of dataflow : $id")
        }
        //update dataModel paths and breadcrumbs
        existing = dataFlowService.updatePaths(existing) as DataFlow
        existing.source = dataFlowService.updateDerivedProperties(existing.source) as DataModel
        existing.target = dataFlowService.updateDerivedProperties(existing.target) as DataModel
        ExporterUtils.createExportResponse(mauroPlugin, existing)
    }


    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Audit(title = EditType.IMPORT, description = "Import data flow")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.DATA_FLOW_IMPORT)
    ListResponse<DataFlow> importModel(@NonNull UUID dataModelId, @Body MultipartBody body, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        // check target model is in right state
        DataModel target = dataModelRepository.readById(dataModelId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.BAD_REQUEST, target, "Datamodel with id $dataModelId not found")
        accessControlService.checkRole(Role.EDITOR, target)
        if (target.finalised) {
            ErrorHandler.handleError(HttpStatus.UNPROCESSABLE_ENTITY, "Target model is finalised: $target.id")
        }

        List<ModelItem> modelItems = dataFlowService.importModelItem(JsonDataFlowImporterPlugin, target, body, namespace, name, version).findAll {
            it.domainType == DataFlow.class.simpleName && (it as DataFlow).target?.id == dataModelId
        }

        List<DataFlow> saved = modelItems.each {imp ->
            log.info '** about to saveWithContentBatched... dataFlow **'

            dataFlowContentRepository.saveWithContent(imp as DataFlow)
            log.info '** finished saveWithContentBatched ** dataFlow'
        } as List<DataFlow>

        log.info '** finished saveWithContentBatched ** dataFlow'
        ListResponse.from(saved.collect {dataFlow ->
            show(dataFlow.id)
        })

    }
    @Override
    ListResponse<DataFlow> importModel(@NonNull UUID dataModelId, @Body io.micronaut.http.client.multipart.MultipartBody body, @Nullable String namespace,
                                       @Nullable String name, @Nullable String version) {
        throw new Exception("Client version of import model has been called.. hint client MultipartBody ")
    }
}
