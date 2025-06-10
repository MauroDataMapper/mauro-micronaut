package org.maurodata.api.datamodel

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.api.model.AdministeredItemApi
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
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
interface EnumerationValueApi extends AdministeredItemApi<EnumerationValue, DataType> {

    @Get(Paths.ENUMERATION_VALUE_ID)
    EnumerationValue show(UUID dataModelId, UUID enumerationTypeId, UUID id)

    @Post(Paths.ENUMERATION_VALUE_LIST)
    EnumerationValue create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue)

    @Put(Paths.ENUMERATION_VALUE_ID)
    EnumerationValue update(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @NonNull EnumerationValue enumerationValue)

    @Delete(Paths.ENUMERATION_VALUE_ID)
    HttpResponse delete(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @Nullable EnumerationValue enumerationValue)

    @Get(Paths.ENUMERATION_VALUE_LIST)
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId)

    @Get(Paths.ENUMERATION_VALUE_LIST_PAGED)
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId, @Nullable PaginationParams params)
}
