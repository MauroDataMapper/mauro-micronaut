package org.maurodata.plugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import jakarta.inject.Singleton

@CompileStatic
@Slf4j
@Singleton
class MauroPluginService {

    // Plugins from the main ApplicationContext during start up
    @Inject
    List<MauroPlugin> mauroPlugins

    List<ClassLoader> getClassLoaders() {
        Set<ClassLoader> distinctClassLoaders = new LinkedHashSet<>(10)

        mauroPlugins.forEach {
            distinctClassLoaders.add(it.getClass().getClassLoader())
        }
        return distinctClassLoaders as List<ClassLoader>
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name, String version) {
        (P) listPlugins().find {
            cls.isInstance(it) && it.namespace == namespace && it.name == name && (!version || (it.version == version))
        }
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name) {
        (P) listPlugins().findAll {
            cls.isInstance(it) && it.namespace == namespace && it.name == name
        }.sort {MauroPlugin plugin -> plugin.version}?.find()
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String name) {
        (P) listPlugins().findAll {
            it.name == name
        }.sort {MauroPlugin plugin -> plugin.version}?.find()
    }

    MauroPlugin getPlugin(String namespace, String name) {
        (MauroPlugin) listPlugins().findAll {
            it.namespace == namespace && it.name == name
        }.sort {MauroPlugin plugin -> plugin.version}?.find()
    }

    MauroPlugin getPlugin(String namespace, String name, String version) {
        (MauroPlugin) listPlugins().find {
            it.namespace == namespace && it.name == name && it.version == version
        }
    }

    List<MauroPlugin> listPlugins() {
        mauroPlugins.asList()
    }

    List<MauroPlugin> listStandardPlugins() {
        final ClassLoader apiClassLoader = getClass().getClassLoader()
        mauroPlugins.asList().findAll {
            it.class.getClassLoader() == apiClassLoader
        } as List<MauroPlugin>
    }

    <P> List<P> listPlugins(Class<P> pluginType) {
        listPlugins().findAll {
            pluginType.isInstance(it)
        } as List<P>
    }

    <P> List<P> listStandardPlugins(Class<P> pluginType) {
        final ClassLoader apiClassLoader = getClass().getClassLoader()
        listStandardPlugins().findAll {
            pluginType.isInstance(it) && it.class.getClassLoader() == apiClassLoader
        } as List<P>
    }

    <P> List<MauroPluginDTO> listPluginsAsDTO(Class<P> pluginType) {
        mauroPlugins.findAll {
            pluginType.isInstance(it)
        }.collect {
            MauroPluginDTO.fromPlugin(it)
        }
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


}
