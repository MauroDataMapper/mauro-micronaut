package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
import uk.ac.ox.softeng.mauro.web.ListResponse

import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import uk.ac.ox.softeng.mauro.web.PaginationParams

@MauroApi
interface DataElementApi extends AdministeredItemApi<DataElement, DataClass> {

    @Get(Paths.DATA_ELEMENT_ID)
    DataElement show(UUID dataModelId, UUID dataClassId, UUID id)

    @Post(Paths.DATA_ELEMENT_LIST)
    DataElement create(UUID dataModelId, UUID dataClassId, @Body @NonNull DataElement dataElement)

    @Put(Paths.DATA_ELEMENT_ID)
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement)

    @Delete(Paths.DATA_ELEMENT_ID)
    HttpResponse delete(UUID dataModelId, UUID dataClassId, UUID id, @Body @Nullable DataElement dataElement)

    @Get(Paths.DATA_ELEMENT_SEARCH)
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId, @Nullable PaginationParams params)

    @Get(Paths.DATA_ELEMENT_DOI)
    Map doi(UUID id)
}
