package uk.ac.ox.softeng.mauro.plugin

import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

class MauroPluginService {

    static ServiceLoader<MauroPlugin> mauroPlugins = ServiceLoader.load(MauroPlugin)

    static <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name, String version) {
        (P) mauroPlugins.find {
            cls.isInstance(it) && it.namespace == namespace && it.name == name && it.version == version
        }
    }

    static <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name) {
        (P) mauroPlugins.findAll {
            cls.isInstance(it) && it.namespace == namespace && it.name == name
        }.sort {P plugin -> plugin.version}.first()
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

}
