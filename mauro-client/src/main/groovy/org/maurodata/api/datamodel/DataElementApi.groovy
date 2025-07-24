package org.maurodata.api.datamodel

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
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

    @Get(Paths.DATA_ELEMENT_LIST_PAGED)
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId, @Nullable PaginationParams params)

    @Get(Paths.DATA_ELEMENT_LIST_PAGED)
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId)

    @Get(Paths.DATA_ELEMENT_IN_MODEL_LIST)
    ListResponse<DataElement> byModelList(UUID dataModelId)

    @Post(Paths.DATA_ELEMENT_COPY)
    DataElement copyDataElement(UUID dataModelId, UUID dataClassId, UUID otherModelId,  UUID otherDataClassId, UUID dataElementId)


    @Get(Paths.DATA_ELEMENT_DOI)
    Map doi(UUID id)
}
