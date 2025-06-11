package org.maurodata.api.importer

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths

import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get

@MauroApi
interface ImporterApi {

    // TODO: Update interface to handle a more intelligent rendering of this information
    @Get(Paths.IMPORTER_PARAMS)
    Map<String, Object> getImporterParameters(String namespace, String name, @Nullable String version)

}
