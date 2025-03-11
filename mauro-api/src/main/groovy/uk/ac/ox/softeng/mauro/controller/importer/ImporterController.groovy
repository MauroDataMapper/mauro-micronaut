package uk.ac.ox.softeng.mauro.controller.importer

import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.importer.ImporterApi
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.service.plugin.PluginService

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import jakarta.inject.Inject

@CompileStatic
@Controller()
@Secured(SecurityRule.IS_ANONYMOUS)
class ImporterController implements ImporterApi {

    @Inject
    MauroPluginService mauroPluginService


    // TODO: Update interface to handle a more intelligent rendering of this information
    @Get(Paths.IMPORTER_PARAMS)
    Map<String, Object> getImporterParameters(String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        PluginService.handlePluginNotFound(mauroPlugin, namespace, name)

        [
            importer: [
                name: mauroPlugin.name,
                version: mauroPlugin.version,
                displayName: mauroPlugin.displayName,
                namespace: mauroPlugin.namespace,
                providerType: mauroPlugin.providerType,
                paramClassType: mauroPlugin.importParametersClass().name,
                canImportMultipleDomains: mauroPlugin.canImportMultipleDomains
            ],
            parameterGroups: mauroPlugin.calculateParameterGroups()
        ]

    }
}
