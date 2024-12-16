package uk.ac.ox.softeng.mauro.api.dataflow

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
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
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.client.annotation.Client


@CompileStatic
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataFlows/')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataFlowApi extends AdministeredItemApi<DataFlow, DataModel> {

    @Get(value = Paths.ID_ROUTE)
    DataFlow show(@NonNull UUID id)

    @Post
    DataFlow create(@NonNull UUID dataModelId, @Body @NonNull DataFlow dataFlow)

    @Put(value = Paths.ID_ROUTE)
    DataFlow update(@NonNull UUID id, @Body @NonNull DataFlow dataFlow)

    @Delete(value = Paths.ID_ROUTE)
    HttpStatus delete(@NonNull UUID dataModelId, @NonNull UUID id, @Body @Nullable DataFlow dataFlow)

    @Get
    ListResponse<DataFlow> list(@NonNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type)

}
