package org.maurodata.api.dataflow

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.dataflow.Type
import org.maurodata.domain.datamodel.DataModel
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

    @Get(Paths.DATA_FLOW_LIST_PAGED)
    ListResponse<DataFlow> list(@NonNull UUID dataModelId, @Nullable @QueryValue(Paths.TYPE_QUERY) Type type, @Nullable PaginationParams params)

}
