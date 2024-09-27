package uk.ac.ox.softeng.mauro.controller.importer

import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

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
class ImporterController {

    @Inject
    MauroPluginService mauroPluginService


    // TODO: Update interface to handle a more intelligent rendering of this information
    @Get('/importer/parameters/{namespace}/{name}/{version}')
    Map<String, Object> getImporterParameters(String namespace, String name, @Nullable String version) {

        ModelImporterPlugin mauroPlugin = mauroPluginService.getPlugin(ModelImporterPlugin, namespace, name, version)
        mauroPluginService.handlePluginNotFound(mauroPlugin, namespace, name)

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
