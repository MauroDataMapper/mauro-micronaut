package uk.ac.ox.softeng.mauro.controller.admin

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.security.AccessControlService

@CompileStatic
@Controller('/admin')
class AdminController {

    @Inject
    MauroPluginService mauroPluginService

    @Inject
    AccessControlService accessControlService

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules() {
        accessControlService.checkAuthenticated()

        mauroPluginService.getModulesList()
    }


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers() {
        accessControlService.checkAuthenticated()

        mauroPluginService.listPlugins(ModelImporterPlugin)
    }


    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters() {
        mauroPluginService.listPlugins(ModelExporterPlugin)
    }

}
