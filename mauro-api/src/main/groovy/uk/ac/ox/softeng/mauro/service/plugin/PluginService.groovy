package uk.ac.ox.softeng.mauro.service.plugin

import uk.ac.ox.softeng.mauro.plugin.MauroPlugin

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

class PluginService {
    static void handlePluginNotFound(MauroPlugin mauroPlugin, String namespace, String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }
    }


}
