package org.maurodata.service.plugin

import org.maurodata.plugin.MauroPlugin

import groovy.transform.CompileStatic
import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import org.maurodata.plugin.importer.ModelItemImporterPlugin

@CompileStatic
class PluginService {
    static void handlePluginNotFound(MauroPlugin mauroPlugin, String namespace, String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }
    }

     static <P extends MauroPlugin> void handlePluginNotFound(MauroPlugin mauroPlugin, Class<P> pluginCls,  String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Plugin with type with type: ${pluginCls.simpleName}, name: ${name} not found")
        }
    }
}
