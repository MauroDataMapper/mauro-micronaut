package org.maurodata.plugin

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@CompileStatic
@Slf4j
class MauroPluginUtil {

    static Path findProjectRoot(final Path start) {
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

    static Path findAppRoot(final Path start) {
        Path current = start
        while (current != null) {
            if (Files.exists(current.resolve("resources")) ||
                Files.exists(current.resolve("plugins"))
            ) {
                return current
            }
            current = current.getParent()
        }
        return null
    }

    static Path getPluginsDirPath() throws URISyntaxException {
        URL url = MauroPluginUtil.getProtectionDomain().getCodeSource().getLocation()
        Path baseDirPath = Paths.get(url.toURI())

        final Path pluginsDirPath

        if (Files.isDirectory(baseDirPath)) {
            // Application is in an IDE
            pluginsDirPath = findProjectRoot(baseDirPath)?.resolve("plugins")
            log.debug("Application IDE Plugin base directory ${baseDirPath}")
        } else {
            // Application is in a packaged jar
            pluginsDirPath = findAppRoot(baseDirPath.getParent())?.resolve("plugins")
            log.debug("Application Plugin base directory ${baseDirPath}")
        }

        return pluginsDirPath
    }
}
