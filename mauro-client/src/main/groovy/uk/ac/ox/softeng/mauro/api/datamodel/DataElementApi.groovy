package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataElement
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
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataClasses/{dataClassId}/dataElements')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataElementApi extends AdministeredItemApi<DataElement, DataClass> {

    @Get('/{id}')
    DataElement show(UUID dataModelId, UUID dataClassId, UUID id)

    @Post
    DataElement create(UUID dataModelId, UUID dataClassId, @Body @NonNull DataElement dataElement)

    @Put('/{id}')
    DataElement update(UUID dataModelId, UUID dataClassId, UUID id, @Body @NonNull DataElement dataElement)

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID dataClassId, UUID id, @Body @Nullable DataElement dataElement)

    @Get
    ListResponse<DataElement> list(UUID dataModelId, UUID dataClassId)


}
