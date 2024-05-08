package uk.ac.ox.softeng.mauro.controller.admin

import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Inject

@CompileStatic
@Controller('/admin')
class AdminController {

    @Inject
    MauroPluginService mauroPluginService

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules() {
        mauroPluginService.getModulesList()
    }


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers() {
        mauroPluginService.listPlugins(ModelImporterPlugin)
    }


    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters() {
        mauroPluginService.listPlugins(ModelExporterPlugin)
    }

}
