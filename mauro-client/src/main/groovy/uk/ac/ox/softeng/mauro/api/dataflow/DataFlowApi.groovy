package uk.ac.ox.softeng.mauro.api.dataflow

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.dataflow.DataFlow
import uk.ac.ox.softeng.mauro.domain.dataflow.Type
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue


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

}
