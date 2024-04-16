package uk.ac.ox.softeng.mauro.plugin

import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

class MauroPluginService {

    static ServiceLoader<MauroPlugin> mauroPlugins = ServiceLoader.load(MauroPlugin)

    static MauroPlugin getPlugin(String namespace, String name, String version) {
        (MauroPlugin) mauroPlugins.find {
            it.namespace == namespace && it.name == name && it.version == version
        }
    }

    static MauroPlugin getPlugin(String namespace, String name) {
        (MauroPlugin) mauroPlugins.findAll {
            it.namespace == namespace && it.name == name
        }.sort {MauroPlugin plugin -> plugin.version}.first()
    }

    static List<MauroPlugin> listPlugins() {
        mauroPlugins.asList()
    }

    static List<MauroPlugin> listPlugins(Class<?> pluginType) {
        mauroPlugins.findAll {
            pluginType.isInstance(it)
        }
    }

    static List<ModelImporterPlugin> listImporterPlugins() {
        mauroPlugins.findAll {
            it instanceof ModelImporterPlugin
        }
    }


}
