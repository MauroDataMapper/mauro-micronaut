package org.maurodata.controller.selfclassifier

import org.maurodata.api.Paths
import org.maurodata.api.selfclassifier.SelfClassifierApi
import org.maurodata.audit.Audit
import org.maurodata.controller.model.AdministeredItemReader

import org.maurodata.persistence.cache.FacetCacheableRepository.MetadataCacheableRepository
import org.maurodata.persistence.facet.MetadataRepository
import org.maurodata.persistence.model.PathRepository
import org.maurodata.persistence.profile.DynamicProfileService
import org.maurodata.plugin.MauroPluginDTO
import org.maurodata.plugin.MauroPluginService
import org.maurodata.security.AccessControlService

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.swagger.v3.oas.annotations.Operation
import jakarta.inject.Inject

@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@Slf4j
class SelfClassifierController implements AdministeredItemReader, SelfClassifierApi {

    @Inject
    AccessControlService accessControlService

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    DynamicProfileService dynamicProfileService

    @Inject
    MetadataRepository metadataRepository

    @Inject
    MetadataCacheableRepository metadataCacheableRepository

    @Inject
    PathRepository pathRepository

    SelfClassifierController() {}

    @Audit
    @Operation(summary = "List the classifying profiles", description = "Returns the classifying profiles.")
    @Get(Paths.SELF_CLASSIFIER_PROVIDERS)
    List<MauroPluginDTO> classifyingProviders(@Nullable String pluginKind, @Nullable String classifierNamespace, @Nullable String classifierLabel) {
        if (classifierNamespace && classifierLabel) {
            return mauroPluginService.getClassifyingPlugins(pluginKind, classifierNamespace, classifierLabel).collect {MauroPluginDTO.fromPlugin(it)}
        }
        if (classifierNamespace) {
            return mauroPluginService.getClassifyingPlugins(pluginKind, classifierNamespace).collect {MauroPluginDTO.fromPlugin(it)}
        }
        mauroPluginService.getClassifyingPlugins(pluginKind).collect {MauroPluginDTO.fromPlugin(it)}
    }
}
