package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface DataTypeApi extends AdministeredItemApi<DataType, DataModel> {

    @Get(Paths.DATA_TYPE_ID)
    DataType show(UUID dataModelId, UUID id)

    @Post(Paths.DATA_TYPE_LIST)
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType)

    @Put(Paths.DATA_TYPE_ID)
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType)

    @Delete(Paths.DATA_TYPE_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType)

    @Get(Paths.DATA_TYPE_LIST)
    ListResponse<DataType> list(UUID dataModelId)

}
