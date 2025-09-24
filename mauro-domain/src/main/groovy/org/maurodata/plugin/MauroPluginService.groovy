package org.maurodata.plugin

import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.event.ShutdownEvent
import io.micronaut.runtime.event.annotation.EventListener
import jakarta.inject.Inject
import jakarta.inject.Singleton

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

@Slf4j
@Singleton
@Context
class MauroPluginService {

    // Plugins from the main ApplicationContext during start up
    @Inject
    List<MauroPlugin> mauroPlugins

    ApplicationContext mainContext

    // Plugins loaded from plugins directory
    final static List<MauroPlugin> loadedMauroPlugins = []

    // All of the ClassLoaders in use
    final static List<ClassLoader> classLoaders = []

    static boolean loadedAlready = false

    @Inject
    MauroPluginService(ApplicationContext mainContext) {
        this.mainContext = mainContext
        if (!loadedAlready) {
            loadedAlready = true
            detectPluginsInDirectory()
        }
    }

    @EventListener
    void onShutdown(final ShutdownEvent event) {
        classLoaders.clear()
        loadedMauroPlugins.clear()
        loadedAlready = false
    }

    private void detectPluginsInDirectory() {

        URL url = getClass().getProtectionDomain().getCodeSource().getLocation()
        Path baseDirPath = Paths.get(url.toURI())

        final Path pluginsDirPath

        if (Files.isDirectory(baseDirPath)) {
            // Application is in an IDE
            pluginsDirPath = findProjectRoot(baseDirPath)?.resolve("plugins")
        } else {
            // Application is in a packaged jar
            pluginsDirPath = baseDirPath.getParent().resolve("plugins")
        }

        if (pluginsDirPath == null) {
            log.error("Failed to locate plugins directory")
            return
        }

        if (Files.exists(pluginsDirPath)) {
            loadPlugins(pluginsDirPath)
        }
    }

    private static Path findProjectRoot(final Path start) {
        Path current = start
        while (current != null) {
            if (Files.exists(current.resolve("build.gradle")) ||
                Files.exists(current.resolve("pom.xml"))) {
                return current
            }
            current = current.getParent()
        }
        return null
    }

    private void loadPlugins(final Path pluginsDirPath) {
        log.trace("Loading plugins")
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(pluginsDirPath)) {
            for (Path dir : dirs) {
                if (Files.isDirectory(dir)) {
                    loadPlugin(dir)
                }
            }
        }
    }

    /*
    Load a plugin by:
    Constructing a new ClassLoader from the JARS/resources found in the plugin directory and sub-directories
    Remembering the ClassLoaders so that anything that needs to look for resources globally can find them (e.g. Flyway)
    Creating an ApplicationContext which uses that ClassLoader to construct new classes
    Finding all new beans of type MauroPlugin
    Adding these new beans to the main ApplicationContext if they haven't been already
    Adding them to loadedMauroPlugins for plugins look-up
     */

    private void loadPlugin(final Path pluginDir) {

        log.trace("Loading plugin ${pluginDir.toString()}")

        final ClassLoader apiClassLoader = getClass().getClassLoader()

        final List<URL> urls = []
        try (Stream<Path> stream = Files.walk(pluginDir)) {
            stream.forEach {Path p ->
                try {
                    urls.add(p.toUri().toURL())
                } catch (MalformedURLException e) {
                    throw new UncheckedIOException(e)
                }
            }
        }

        URLClassLoader pluginLoader = new URLClassLoader(
            (URL[]) urls.toArray(new URL[0]),
            apiClassLoader
        )

        classLoaders.add(pluginLoader)

        ApplicationContext applicationContext = ApplicationContext.run(
            pluginLoader
        ) as ApplicationContext

        applicationContext.getBeansOfType(MauroPlugin).forEach {MauroPlugin it ->
            if (mainContext.getBeansOfType(it.class as Class<Object>).isEmpty()) {
                log.trace("Loaded plugin ${it.getClass().simpleName}")
                loadedMauroPlugins.add(it)
            }
        }
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name, String version) {
        (P) listPlugins().find {
            cls.isInstance(it) && it.namespace == namespace && it.name == name && (!version || (it.version == version))
        }
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String namespace, String name) {
        (P) listPlugins().findAll {
            cls.isInstance(it) && it.namespace == namespace && it.name == name
        }.sort {P plugin -> plugin.version}?.find()
    }

    <P extends MauroPlugin> P getPlugin(Class<P> cls, String name) {
        (P) listPlugins().findAll {
            it.name == name
        }.sort {P plugin -> plugin.version}?.find()
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
        mauroPlugins.asList() + loadedMauroPlugins.asList()
    }

    List<MauroPlugin> listStandardPlugins() {
        mauroPlugins.asList() - loadedMauroPlugins.asList()
    }

    <P> List<P> listPlugins(Class<P> pluginType) {
        listPlugins().findAll {
            pluginType.isInstance(it)
        } as List<P>
    }

    <P> List<P> listStandardPlugins(Class<P> pluginType) {
        listStandardPlugins().findAll {
            pluginType.isInstance(it)
        } as List<P>
    }

    <P> List<MauroPluginDTO> listPluginsAsDTO(Class<P> pluginType) {
        mauroPlugins.findAll {
            pluginType.isInstance(it)
        }.collect {
            MauroPluginDTO.fromPlugin(it)
        } + loadedMauroPlugins.findAll {
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
