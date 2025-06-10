package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.MauroApi
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
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
