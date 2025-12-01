package org.maurodata.api.datamodel

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
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

    @Get(Paths.DATA_TYPE_LIST_PAGED)
    ListResponse<DataType> list(UUID dataModelId, @Nullable PaginationParams params)

    @Get(Paths.DATA_TYPE_DATA_ELEMENTS)
    ListResponse<DataElement> listDataElementsForType(UUID dataModelId, UUID dataTypeId)

    @Get(Paths.DATA_TYPE_DATA_ELEMENTS_PAGED)
    ListResponse<DataElement> listDataElementsForType(UUID dataModelId, UUID dataTypeId, @Nullable PaginationParams params)

    @Get(Paths.PRIMITIVETYPE_DOI)
    Map primitiveTypeDoi(UUID id)

    @Get(Paths.ENUMERATIONTYPE_DOI)
    Map enumerationTypeDoi(UUID id)

    @Get(Paths.REFERENCETYPE_DOI)
    Map referenceTypeDoi(UUID id)
}
