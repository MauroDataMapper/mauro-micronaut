package org.maurodata.controller

import io.micronaut.context.annotation.Property
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.MauroPluginUtil
import org.maurodata.profile.Profile

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.context.ApplicationContext
import io.micronaut.context.ApplicationContextBuilder
import io.micronaut.context.ApplicationContextConfigurer
import io.micronaut.context.BeanDefinitionRegistry
import io.micronaut.context.RuntimeBeanDefinition
import io.micronaut.context.annotation.ContextConfigurer
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.inject.BeanDefinition
import io.micronaut.core.io.service.ServiceDefinition
import io.micronaut.core.io.service.SoftServiceLoader
import io.micronaut.inject.BeanDefinitionReference
import picocli.CommandLine

import java.lang.annotation.Annotation
import java.lang.reflect.Method
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

@CompileStatic
@Slf4j
@ContextConfigurer
class MauroApplicationContextConfigurer implements ApplicationContextConfigurer {

    @Override
    void configure(ApplicationContextBuilder builder) {
        System.out.println("""
 __  __ 
|  \\/  | __ _ _   _ _ __ ___ 
| |\\/| |/ _` | | | | '__/ _ \\
| |  | | (_| | | | | | | (_) |
|_|  |_|\\__,_|\\__,_|_|  \\___/
""".toString())
    }

    @Override
    void configure(ApplicationContext applicationContext) {

        // Could be enabled in Docker-type deployments;
        // Should be disabled when running in development mode via gradle, or in live environments where no additional plugins will be required
        boolean autoRegisterPlugins = applicationContext.environment
            .getProperty('mauro.plugins.autoregister', Boolean)
            .orElse(false)

        log.debug("autoRegisterPlugins = {}", autoRegisterPlugins)
        if(autoRegisterPlugins) {

            URL url = getClass().getProtectionDomain().getCodeSource().getLocation()
            Path baseDirPath = Paths.get(url.toURI())

            final Path pluginsDirPath

            if (Files.isDirectory(baseDirPath)) {
                // Application is in an IDE
                pluginsDirPath = MauroPluginUtil.findProjectRoot(baseDirPath)?.resolve("plugins")
                log.debug("Application IDE Plugin base directory ${baseDirPath}")
            } else {
                // Application is in a packaged jar
                pluginsDirPath = MauroPluginUtil.findAppRoot(baseDirPath.getParent())?.resolve("plugins")
                log.debug("Application Plugin base directory ${baseDirPath}")
            }

            if (pluginsDirPath == null) {
                log.warn("Failed to locate plugins directory")
                return
            }

            if (Files.exists(pluginsDirPath)) {
                loadPlugins(pluginsDirPath, applicationContext)
            }
        }
    }

    private void loadPlugins(final Path pluginsDirPath, final ApplicationContext applicationContext) {
        log.debug("Loading plugins")
        try (DirectoryStream<Path> files = Files.newDirectoryStream(pluginsDirPath)) {
            for (Path file : files) {
                if (file.getFileName().toString().startsWith('.')) {continue}
                if (Files.isDirectory(file)) {
                    loadPlugin(file, applicationContext)
                } else {
                    if (Files.isRegularFile(file) && file.getFileName().toString().endsWith(".jar")) {
                        loadPlugin(file, applicationContext)
                    }
                }
            }
        }
    }

    private void loadPlugin(final Path pluginLocation, final ApplicationContext applicationContext) {

        log.debug("Loading plugin ${pluginLocation}")

        final ClassLoader apiClassLoader = getClass().getClassLoader()

        final List<URL> urls = []
        if (Files.isDirectory(pluginLocation)) {
            try (Stream<Path> stream = Files.walk(pluginLocation)) {
                stream.forEach {Path p ->
                    try {
                        urls.add(p.toUri().toURL())
                    } catch (MalformedURLException e) {
                        throw new UncheckedIOException(e)
                    }
                }
            }
        } else {
            try {
                urls.add(pluginLocation.toUri().toURL())
            } catch (MalformedURLException e) {
                throw new UncheckedIOException(e)
            }
        }

        if (urls.isEmpty()) {
            return
        }

        URLClassLoader pluginLoader = new URLClassLoader(
            urls.toArray(new URL[0]) as URL[],
            apiClassLoader
        )

        SoftServiceLoader<BeanDefinitionReference> loader =
            SoftServiceLoader.load(
                BeanDefinitionReference.class,
                pluginLoader
            )

        for (ServiceDefinition<BeanDefinitionReference> definition : loader) {
            if (!definition.isPresent()) { continue }

            String className = definition.getName()

            try {
                Class<?> klass = Class.forName(className, false, pluginLoader)

                if(klass.getClassLoader() != pluginLoader) { continue }

                BeanDefinitionReference ref = definition.load()

                BeanDefinition<?> beanDefinition = ref.load()

                Class<?> beanType = ref.getBeanType()

                ((BeanDefinitionRegistry) applicationContext).registerBeanDefinition(beanDefinition as RuntimeBeanDefinition<Object>)

                if (Profile.class.isAssignableFrom(beanType)) {
                    log.info("Profile: ${ref}")
                } else if (MauroPlugin.class.isAssignableFrom(beanType)) {
                    log.info("MauroPlugin: ${ref}")
                } else {
                    if (ref.hasAnnotation(Controller)) {
                        log.info("Controller: ${ref}")
                        Method[] methods = beanType.getMethods()
                        methods.each {Method method ->

                            Annotation annotation_GET = method.getAnnotation(Get)
                            if (annotation_GET != null) {
                                final String value = ((Get) annotation_GET).value()
                                if (value != null) {
                                    log.info("GET: ${value}")
                                }
                            }

                            Annotation annotation_POST = method.getAnnotation(Post)
                            if (annotation_POST != null) {
                                final String value = ((Post) annotation_POST).value()
                                if (value != null) {
                                    log.info("POST: ${value}")
                                }
                            }

                            Annotation annotation_PUT = method.getAnnotation(Put)
                            if (annotation_PUT != null) {
                                final String value = ((Put) annotation_PUT).value()
                                if (value != null) {
                                    log.info("PUT: ${value}")
                                }
                            }

                            Annotation annotation_DELETE = method.getAnnotation(Delete)
                            if (annotation_DELETE != null) {
                                final String value = ((Delete) annotation_DELETE).value()
                                if (value != null) {
                                    log.info("DELETE: ${value}")
                                }
                            }
                        }
                    } else if (ref.hasAnnotation(CommandLine.Command)) {
                        log.info("Command line: ${ref}")
                    } else {
                        log.trace("Bean: ${ref}")
                    }
                }
            } catch (Throwable th) {
                log.warn("Plugin-specific failure when loading ${className} from ${pluginLocation}: ${th.getMessage()}")
            }
        }

    }
}
