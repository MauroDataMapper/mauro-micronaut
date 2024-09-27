package uk.ac.ox.softeng.mauro.plugin

import io.micronaut.http.HttpStatus
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class MauroPluginService {

    @Inject
    List<MauroPlugin> mauroPlugins

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name, String version) {
        (P) mauroPlugins.find {
            cls.isInstance(it) && it.namespace == namespace && it.name == name && it.version == version
        }
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name) {
        (P) mauroPlugins.findAll {
            cls.isInstance(it) && it.namespace == namespace && it.name == name
        }.sort {P plugin -> plugin.version}.first()
    }

    MauroPlugin getPlugin(String namespace, String name) {
        (MauroPlugin) mauroPlugins.findAll {
            it.namespace == namespace && it.name == name
        }.sort {MauroPlugin plugin -> plugin.version}.first()
    }

    MauroPlugin getPlugin(String namespace, String name, String version) {
        (MauroPlugin) mauroPlugins.find {
            it.namespace == namespace && it.name == name && it.version == version
        }
    }

    List<MauroPlugin> listPlugins() {
        mauroPlugins.asList()
    }

    <P> List<P> listPlugins(Class<P> pluginType) {
        mauroPlugins.findAll {
            pluginType.isInstance(it)
        } as List<P>
    }

    List<LinkedHashMap<String, String>> getModulesList() {
        return (ModuleLayer.boot().modules().collect {
            ["name"   : it.name,
             "version": it.descriptor.version().get().toString()]
        } +
         listPlugins().collect {
             ["name"   : it.name,
              "version": it.version]
         }).sort {it.name}
    }

    static void handlePluginNotFound(MauroPlugin mauroPlugin, String namespace, String name) {
        if (!mauroPlugin) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Model import plugin with namespace: ${namespace}, name: ${name} not found")
        }
    }


}
