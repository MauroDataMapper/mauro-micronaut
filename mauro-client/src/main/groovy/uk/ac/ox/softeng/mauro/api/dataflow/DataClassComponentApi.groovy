package uk.ac.ox.softeng.mauro.api.dataflow

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.web.ListResponse

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Header
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataClassComponentApi extends AdministeredItemApi<DataClassComponent, DataFlow> {

    @Get(value = Paths.ID_ROUTE)
    DataClassComponent show(@NonNull UUID dataFlowId, @NonNull UUID id)

    @Post
    DataClassComponent create(@NonNull UUID dataFlowId, @Body @NonNull DataClassComponent dataClassComponent)

    @Put(value = Paths.ID_ROUTE)
    DataClassComponent update(@NonNull UUID id, @Body @NonNull DataClassComponent dataClassComponent)

    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID id, @Body @Nullable DataClassComponent dataClassComponent)

    @Get
    ListResponse<DataClassComponent> list(@NonNull UUID dataFlowId)

    @Put(value = Paths.SOURCE_DATA_CLASS_ROUTE)
    DataClassComponent update(@NonNull UUID dataModelId, @NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Put(value = Paths.TARGET_DATA_CLASS_ROUTE)
    DataClassComponent update(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.TARGET_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID id, @NonNull UUID dataClassId)

    @Delete(value = Paths.SOURCE_DATA_CLASS_ROUTE)
    HttpStatus delete(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataClassId)

}
