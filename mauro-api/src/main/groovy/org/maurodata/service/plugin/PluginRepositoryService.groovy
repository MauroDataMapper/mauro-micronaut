package org.maurodata.service.plugin

import org.maurodata.plugin.MauroPluginUtil

import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import groovy.xml.XmlSlurper
import groovy.xml.slurpersupport.GPathResult
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.exceptions.HttpStatusException
import jakarta.inject.Singleton

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern

@CompileStatic
@Singleton
@Slf4j
class PluginRepositoryService {

    private static final String REPO_BASE = "https://mauro-repository.com"
    private static final String PLUGIN_PATH = "/libs-snapshot-local/org/maurodata/plugins/"
    private static final String API_BASE = "${REPO_BASE}/api/maven/details${PLUGIN_PATH}"

    private final HttpClient httpClient

    PluginRepositoryService(@Client("/") HttpClient httpClient) {
        this.httpClient = httpClient
    }

    List<Map<String, String>> listAvailablePlugins() {

        List<String> plugins = getPluginDirectories()

        plugins.collect {plugin ->
            String version = getLatestVersion(plugin)
            if (!version) return null

            String[] jarUrl = resolveJarUrl(plugin, version)
            if (!jarUrl) return null

            [
                plugin : plugin,
                version: version,
                url    : jarUrl[0]
            ] as Map<String, String>
        }.findAll {it != null}
    }

    private List<String> getPluginDirectories() {

        String json = httpClient.toBlocking()
            .retrieve(API_BASE)

        JsonSlurper slurper = new JsonSlurper()

        Map<String, Object> parsed =
            (Map<String, Object>) slurper.parseText(json)

        List<Map<String, Object>> files =
            (List<Map<String, Object>>) parsed.get("files")

        List<String> directories = new ArrayList<>()

        for (Map<String, Object> file : files) {

            Object type = file.get("type")
            Object name = file.get("name")

            if ("DIRECTORY".equals(type) && name instanceof String) {
                directories.add((String) name)
            }
        }

        return directories
    }

    private String getLatestVersion(String plugin) {
        String metadataUrl = "${REPO_BASE}${PLUGIN_PATH}${plugin}/maven-metadata.xml"

        try {
            String xml = httpClient.toBlocking().retrieve(metadataUrl)

            XmlSlurper slurper = new XmlSlurper()
            GPathResult metadata = slurper.parseText(xml)

            GPathResult versioning = metadata.getProperty("versioning") as GPathResult
            GPathResult latest = versioning.getProperty("latest") as GPathResult

            return latest.text()
        }
        catch (Exception ignored) {
            return null
        }
    }

    private String[] resolveJarUrl(String plugin, String version) {

        String resolvedVersion = version

        if (version.endsWith("SNAPSHOT")) {
            resolvedVersion = resolveSnapshotVersion(plugin, version)
            if (resolvedVersion == null) return null
        }

        String moduleFile = resolveModuleFilename(plugin, version, resolvedVersion)
        if (moduleFile == null) return null

        return selectJarFromModule(plugin, version, resolvedVersion, moduleFile)
    }

    private String resolveSnapshotVersion(String plugin, String version) {

        String metadataUrl =
            "${REPO_BASE}${PLUGIN_PATH}${plugin}/${version}/maven-metadata.xml"

        try {
            String xml = httpClient.toBlocking().retrieve(metadataUrl)
            XmlSlurper slurper = new XmlSlurper()
            GPathResult metadata = slurper.parseText(xml)

            GPathResult versioning =
                (GPathResult) metadata.getProperty("versioning")

            GPathResult snapshotVersions =
                (GPathResult) versioning.getProperty("snapshotVersions")

            GPathResult snapshotVersionList =
                (GPathResult) snapshotVersions.getProperty("snapshotVersion")

            for (Object obj : snapshotVersionList) {
                GPathResult entry = (GPathResult) obj

                GPathResult extensionResult = entry.getProperty("extension") as GPathResult
                String extension = extensionResult.text()

                if ("jar".equals(extension)) {

                    GPathResult valueResult = entry.getProperty("value") as GPathResult
                    return valueResult.text()
                }
            }
        }
        catch (Exception ignored) {}

        return null
    }

    private static String resolveModuleFilename(String plugin,
                                                String originalVersion,
                                                String resolvedVersion) {

        if (!originalVersion.endsWith("SNAPSHOT")) {
            return "${plugin}-${originalVersion}.module"
        }

        return "${plugin}-${resolvedVersion}.module"
    }

    private String[] selectJarFromModule(String plugin,
                                         String originalVersion,
                                         String resolvedVersion,
                                         String moduleFile) {

        String moduleUrl =
            "${REPO_BASE}${PLUGIN_PATH}${plugin}/${originalVersion}/${moduleFile}"

        try {
            String moduleJson = httpClient.toBlocking().retrieve(moduleUrl)

            JsonSlurper slurper = new JsonSlurper()
            Map<String, Object> parsed =
                (Map<String, Object>) slurper.parseText(moduleJson)

            List<Map<String, Object>> variants =
                (List<Map<String, Object>>) parsed.get("variants")

            Map<String, Object> selectedVariant = null

            for (String variantName :
                ["shadowRuntimeElements", "runtimeElements", "apiElements"]) {

                for (Map<String, Object> variant : variants) {
                    if (variantName.equals(variant.get("name"))) {
                        selectedVariant = variant
                        break
                    }
                }
                if (selectedVariant != null) break
            }

            if (selectedVariant == null) return null

            List<Map<String, Object>> files =
                (List<Map<String, Object>>) selectedVariant.get("files")

            for (Map<String, Object> file : files) {

                Object nameObj = file.get("name")
                Object urlObj = file.get("url")

                if (nameObj instanceof String &&
                    urlObj instanceof String &&
                    ((String) nameObj).endsWith(".jar")) {

                    String fileName
                    if (originalVersion.endsWith("-SNAPSHOT")) {
                        String baseVersion = originalVersion.replace("-SNAPSHOT", "")
                        String timestampSuffix = resolvedVersion.replaceFirst(/^${baseVersion}-/, "")
                        fileName = (urlObj as String).replaceFirst(/-SNAPSHOT/, "-${timestampSuffix}")
                    } else {
                        fileName = urlObj as String
                    }


                    return [
                        "${REPO_BASE}${PLUGIN_PATH}${plugin}/${originalVersion}/${fileName}",
                        "${REPO_BASE}${PLUGIN_PATH}${plugin}/${originalVersion}/${urlObj}",
                        plugin
                    ] as String[]
                }
            }
        }
        catch (Exception ignored) {}

        return null
    }

    Map<String, Object> installPlugin(String plugin) {
        String version = getLatestVersion(plugin)
        if (!version) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Plugin not found: ' + plugin)
        }

        String[] jarUrl = resolveJarUrl(plugin, version)
        if (!jarUrl) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Could not resolve plugin to a file: ' + plugin)
        }

        final Path pluginsDirPath = MauroPluginUtil.pluginsDirPath

        if (pluginsDirPath == null) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing plugins directory')
        }

        if (!Files.exists(pluginsDirPath)) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Missing plugins directory: ' + pluginsDirPath)
        }

        final String PLUGINS_IS_MOUNTED = System.getenv("PLUGINS_IS_MOUNTED")
        if (PLUGINS_IS_MOUNTED != null && PLUGINS_IS_MOUNTED == 'false') {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Plugins directory is not using persistent storage')
        }

        log.debug("Download ${jarUrl[0]} into ${pluginsDirPath} as ${jarUrl[1]}, removing any other ${jarUrl[2]}")
        return downloadPluginJar(jarUrl, pluginsDirPath)
    }

    private static Map<String, Object> downloadPluginJar(String[] jarUrl, Path dirPath) {
        URL downloadUrl = new URL(jarUrl[0])
        URL saveUrl = new URL(jarUrl[1])
        String plugin = jarUrl[2]

        String path = saveUrl.getPath()
        int lastSlash = path.lastIndexOf('/')
        if (lastSlash == -1) {
            throw new HttpStatusException(HttpStatus.UNPROCESSABLE_ENTITY, 'Malformed URL. Missing path: ' + saveUrl)
        }

        String fileName = path.substring(lastSlash + 1)
        Path filePath = dirPath.resolve(fileName)

        downloadUrl.withInputStream {input ->
            Files.copy(input, filePath, StandardCopyOption.REPLACE_EXISTING)
        }

        // Find all plugin files with the same naming

        Pattern pattern = Pattern.compile(
            '^' + Pattern.quote(plugin) + '-\\d+\\.\\d+\\.\\d+.*\\.jar$'
        )

        List<File> matches = new ArrayList<>()

        File[] files = dirPath.toFile().listFiles()

        if (files != null && files.length > 0) {
            for (File file : files) {
                if (file.isFile()) {
                    String name = file.getName()
                    if (pattern.matcher(name).matches() && !name.equalsIgnoreCase(fileName)) {
                        matches.add(file)
                    }
                }
            }
        }

        matches.forEach {File toDelete ->
            boolean deleted = toDelete.delete()
            if (!deleted) {
                throw new IOException("Failed to remove plugin file: " + toDelete.getName())
            }
        }

        return [
            installed: plugin,
            from     : downloadUrl.toString(),
            as       : fileName,
            removed  : matches.collect {File removed -> removed.getName()} as List<String>
        ] as Map<String, Object>
    }
}
