package uk.ac.ox.softeng.mauro.api.dataflow

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface DataClassComponentApi extends AdministeredItemApi<DataClassComponent, DataFlow> {

    @Get(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent show(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id)

    @Post(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    DataClassComponent create(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_ID)
    HttpStatus delete(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent)

    @Get(Paths.DATA_FLOW_CLASS_COMPONENT_LIST)
    ListResponse<DataClassComponent> list(@NonNull UUID dataModelId, @NonNull UUID dataFlowId)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    DataClassComponent updateSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Put(value = Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    DataClassComponent updateTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_SOURCE_CLASS)
    HttpStatus deleteSource(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.DATA_FLOW_CLASS_COMPONENT_TARGET_CLASS)
    HttpStatus deleteTarget(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

}
