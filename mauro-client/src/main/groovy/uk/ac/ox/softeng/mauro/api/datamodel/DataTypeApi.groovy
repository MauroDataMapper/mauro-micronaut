package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
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
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataTypes')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataTypeApi extends AdministeredItemApi<DataType, DataModel> {

    @Get('/{id}')
    DataType show(UUID dataModelId, UUID id)

    @Post
    DataType create(UUID dataModelId, @Body @NonNull DataType dataType)

    @Put('/{id}')
    DataType update(UUID dataModelId, UUID id, @Body @NonNull DataType dataType)

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID id, @Body @Nullable DataType dataType)

    @Get
    ListResponse<DataType> list(UUID dataModelId)

}
