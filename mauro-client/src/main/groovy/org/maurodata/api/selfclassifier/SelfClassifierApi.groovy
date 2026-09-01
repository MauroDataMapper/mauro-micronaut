package org.maurodata.api.selfclassifier

import org.maurodata.api.MauroApi
import org.maurodata.api.Paths
import org.maurodata.plugin.MauroPluginDTO
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Get

@MauroApi
interface SelfClassifierApi {

    @Get(Paths.SELF_CLASSIFIER_PROVIDERS)
    List<MauroPluginDTO> classifyingProviders(@Nullable String pluginKind, @Nullable String classifierNamespace, @Nullable String classifierLabel)
}
