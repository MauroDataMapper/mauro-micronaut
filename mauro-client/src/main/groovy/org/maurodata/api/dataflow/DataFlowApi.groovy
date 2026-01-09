package org.maurodata.api.dataflow

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.plugin.exporter.DataFlowExporterPlugin
import org.maurodata.plugin.importer.DataFlowImporterPlugin
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

@MauroApi
interface DataFlowApi extends AdministeredItemApi<DataFlow, DataModel> {

    @Get(Paths.DATA_FLOW_ID)
    DataFlow show(@NonNull UUID dataModelId, @NonNull UUID id)

    @Post(Paths.DATA_FLOW_LIST)
    DataFlow create(@NonNull UUID dataModelId, @Body @NonNull DataFlow dataFlow)

    @Put(Paths.DATA_FLOW_ID)
    DataFlow update(@NonNull UUID dataModelId, @NonNull UUID id, @Body @NonNull DataFlow dataFlow)

    @Delete(Paths.DATA_FLOW_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID id, @Body @Nullable DataFlow dataFlow)

    @Get(Paths.DATA_FLOW_LIST)
    ListResponse<DataFlow> list(@NonNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type)

    @Get(Paths.DATA_FLOW_LIST_PAGED)
    ListResponse<DataFlow> list(@NonNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type, @Nullable PaginationParams params)

    @Get(Paths.DATA_FLOW_EXPORTERS)
    List<DataFlowExporterPlugin> dataFlowExporters()

    @Get(Paths.DATA_FLOW_IMPORTERS)
    List<DataFlowImporterPlugin> dataFlowImporters()

    @Get(Paths.DATA_FLOW_EXPORT)
    HttpResponse<byte[]> exportModel(@NonNull UUID dataModelId, @NonNull UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version)


    @ExecuteOn(TaskExecutors.IO)
    @Produces(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.DATA_FLOW_IMPORT)
    ListResponse<DataFlow> importModel(@NonNull UUID dataModelId, @Body MultipartBody body, @Nullable String namespace, @Nullable String name, @Nullable String version)

    // This is the version that will be implemented by the controller
    ListResponse<DataFlow> importModel(@NonNull UUID dataModelId, @Body io.micronaut.http.server.multipart.MultipartBody body, String namespace, String name, @Nullable String version)
}
