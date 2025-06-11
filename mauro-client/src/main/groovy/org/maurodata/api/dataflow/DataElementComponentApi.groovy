package org.maurodata.api.dataflow

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface DataElementComponentApi extends AdministeredItemApi<DataElementComponent, DataClassComponent> {

    @Get(Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id)

    @Post(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST)
    DataElementComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @Body @NonNull DataElementComponent dataElementComponent)

    @Put(Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    DataElementComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @NonNull DataElementComponent dataElementComponent)

    @Delete(Paths.DATA_FLOW_ELEMENT_COMPONENT_ID)
    HttpResponse delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @Body @Nullable DataElementComponent dataElementComponent)

    @Get(Paths.DATA_FLOW_ELEMENT_COMPONENT_LIST)
    ListResponse<DataElementComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId)

    @Put(Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    DataElementComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

    @Put(Paths.DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT)
    DataElementComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

    @Delete(Paths.DATA_FLOW_ELEMENT_COMPONENT_SOURCE_ELEMENT)
    HttpResponse deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

    @Delete(Paths.DATA_FLOW_ELEMENT_COMPONENT_TARGET_ELEMENT)
    HttpResponse deleteTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

}
