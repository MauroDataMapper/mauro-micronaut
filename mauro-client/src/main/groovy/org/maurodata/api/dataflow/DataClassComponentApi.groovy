package org.maurodata.api.dataflow

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.web.ListResponse
import org.maurodata.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.exceptions.HttpException

@MauroApi
interface DataClassComponentApi extends AdministeredItemApi<DataClassComponent, DataFlow> {

    @Get(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id)

    @Post(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    DataClassComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent)

    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    ListResponse<DataClassComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId)

    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_LIST_PAGED)
    ListResponse<DataClassComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Nullable PaginationParams params)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    DataClassComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    DataClassComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    HttpResponse deleteTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId) throws HttpException

}
