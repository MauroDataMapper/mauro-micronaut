package uk.ac.ox.softeng.mauro.api.dataflow

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.dataflow.DataElementComponent
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
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataFlows/{dataFlowId}/dataClassComponents/{dataClassComponentId}/dataElementComponents')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataElementComponentApi extends AdministeredItemApi<DataElementComponent, DataClassComponent> {

    @Get(value = Paths.ID_ROUTE)
    DataElementComponent show(@NonNull UUID dataFlowId, @NonNull UUID id)

    @Post
    DataElementComponent create(@NonNull UUID dataClassComponentId, @Body @NonNull DataElementComponent dataElementComponent)

    @Put(value = Paths.ID_ROUTE)
    DataElementComponent update(@NonNull UUID id, @Body @NonNull DataElementComponent dataElementComponent)

    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID id, @Body @Nullable DataElementComponent dataElementComponent)

    @Get
    ListResponse<DataElementComponent> list(@NonNull UUID dataClassComponentId)

    @Put(value = Paths.SOURCE_DATA_ELEMENT_ROUTE)
    DataElementComponent update(@NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

    @Put(value = Paths.TARGET_DATA_ELEMENT_ROUTE)
    DataElementComponent update(@NonNull UUID dataFlowId, @NonNull UUID dataClassComponentId, @NonNull UUID id, @NonNull UUID dataElementId)

    @Delete(value = Paths.TARGET_DATA_ELEMENT_ROUTE)
    HttpStatus delete(@NonNull UUID id, @NonNull UUID dataElementId)

    @Delete(value = Paths.SOURCE_DATA_ELEMENT_ROUTE)
    HttpStatus delete(@NonNull UUID dataFlowId, @NonNull UUID id, @NonNull UUID dataElementId)

}
