package uk.ac.ox.softeng.mauro.controller.admin

import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

import groovy.transform.CompileStatic
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@CompileStatic
@Controller('/admin')
class AdminController {

    @Get('/modules')
    List<LinkedHashMap<String, String>> modules() {
        MauroPluginService.getModulesList()
    }


    @Get('/providers/importers')
    List<ModelImporterPlugin> importers() {
        MauroPluginService.listPlugins(ModelImporterPlugin)
    }


/*
    @Get('/providers/exporters')
    List<ModelExporterPlugin> exporters() {
        MauroPluginService.listPlugins(ModelExporterPlugin)
    }
*/
}
