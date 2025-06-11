package org.maurodata.service.plugin

import org.maurodata.plugin.MauroPlugin

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException

class PluginService {
    static void handlePluginNotFound(MauroPlugin mauroPlugin, String namespace, String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }
    }
}
