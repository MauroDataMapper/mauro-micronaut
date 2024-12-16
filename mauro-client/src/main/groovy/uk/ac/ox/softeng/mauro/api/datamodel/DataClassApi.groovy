package uk.ac.ox.softeng.mauro.api.datamodel

import uk.ac.ox.softeng.mauro.api.model.AdministeredItemApi
import uk.ac.ox.softeng.mauro.domain.datamodel.DataClass
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
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
@Client('${micronaut.http.services.mauro.url}/dataModels/{dataModelId}/dataClasses')
@Header(name='apiKey', value = '${micronaut.http.services.mauro.apikey}')
interface DataClassApi extends AdministeredItemApi<DataClass, DataModel> {

    @Get('/{id}')
    DataClass show(UUID dataModelId, UUID id)

    @Post
    DataClass create(UUID dataModelId, @Body @NonNull DataClass dataClass)

    @Put('/{id}')
    DataClass update(UUID dataModelId, UUID id, @Body @NonNull DataClass dataClass)

    @Delete('/{id}')
    HttpStatus delete(UUID dataModelId, UUID id, @Body @Nullable DataClass dataClass)

    @Get
    ListResponse<DataClass> list(UUID dataModelId)

    @Get('/{parentDataClassId}/dataClasses/{id}')
    DataClass show(UUID dataModelId, UUID parentDataClassId, UUID id)

    @Post('/{parentDataClassId}/dataClasses')
    DataClass create(UUID dataModelId, UUID parentDataClassId, @Body @NonNull DataClass dataClass)

    @Put('/{parentDataClassId}/dataClasses/{id}')
    DataClass update(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @NonNull DataClass dataClass)

    @Delete('/{parentDataClassId}/dataClasses/{id}')
    HttpStatus delete(UUID dataModelId, UUID parentDataClassId, UUID id, @Body @Nullable DataClass dataClass)

    @Get('/{parentDataClassId}/dataClasses')
    ListResponse<DataClass> list(UUID dataModelId, UUID parentDataClassId)

}
