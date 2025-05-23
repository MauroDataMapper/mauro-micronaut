package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.web.ListResponse
import uk.ac.ox.softeng.mauro.web.PaginationParams

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put

@MauroApi
interface DataClassApi extends AdministeredItemApi<DataClass, DataModel> {

    @Get(Paths.DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID id)

    @Post(Paths.DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass)

    @Put(Paths.DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass)

    @Delete(Paths.DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass)

    @Get(Paths.DATA_CLASS_SEARCH)
    ListResponse<DataClass> list(UUID dataModelId, @Nullable PaginationParams params)

    @Get(Paths.DATA_CLASS_SEARCH)
    ListResponse<DataClass> list(UUID dataModelId)

    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id)

    @Post(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass)

    @Put(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass)

    @Delete(Paths.DATA_CLASS_CHILD_DATA_CLASS_ID)
    HttpResponse delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass)

    @Get(Paths.DATA_CLASS_CHILD_DATA_CLASS_LIST)
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId)

    @Put(Paths.DATA_CLASS_EXTENDS)
    DataClass createExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId)

    @Delete(Paths.DATA_CLASS_EXTENDS)
    DataClass deleteExtension(UUID dataModelId, UUID id, UUID otherModelId, UUID otherClassId)

    @Get(Paths.DATA_CLASS_DOI)
    Map doi(UUID id)
}
