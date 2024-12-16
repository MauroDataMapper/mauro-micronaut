package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.datamodel.EnumerationValue
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
import io.micronaut.http.client.annotation.Client

@CompileStatic
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataTypes/{enumerationTypeId}/enumerationValues')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface EnumerationValueApi extends AdministeredItemApi<EnumerationValue, DataType> {

    @Get('/{id}')
    EnumerationValue show(UUID dataModelId, UUID enumerationTypeId, UUID id)

    @Post
    EnumerationValue create(UUID dataModelId, UUID enumerationTypeId, @Body @NonNull EnumerationValue enumerationValue)

    @Put('/{id}')
    EnumerationValue update(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @NonNull EnumerationValue enumerationValue)

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID enumerationTypeId, UUID id, @Body @Nullable EnumerationValue enumerationValue)

    @Get
    ListResponse<EnumerationValue> list(UUID dataModelId, UUID enumerationTypeId)

}
