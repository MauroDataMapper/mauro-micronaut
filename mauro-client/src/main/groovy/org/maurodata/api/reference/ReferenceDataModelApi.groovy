package org.maurodata.api.reference

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.core.annotation.NonNull
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post

@MauroApi
interface ReferenceDataModelApi {

    @Get(Paths.REFERENCE_DATA_MODELS_LIST)
    List<Map> listAll()

    @Post(Paths.FOLDER_REFERENCE_DATA_MODELS)
    Map create(@NonNull UUID id, @Body @NonNull Map referenceDataModel)
}
