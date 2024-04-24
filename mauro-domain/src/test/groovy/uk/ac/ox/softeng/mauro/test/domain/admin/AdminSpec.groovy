package uk.ac.ox.softeng.mauro.test.domain.admin

import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.MauroPluginService
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

/**
 * Tests for Administration services
 */
@MicronautTest
class AdminSpec extends Specification {

    @Inject
    MauroPluginService mauroPluginService

    def "test the module list"() {
        when:
            List modulesList = mauroPluginService.getModulesList()
            String javaVersion = System.getProperty("java.version")
        then:
            modulesList.size() > 0
            modulesList.find {it["name"] == "JsonDataModelImporterPlugin" && it["version"] == "4.0.0"}
            modulesList.find {it["name"] == "java.base" && it["version"] == javaVersion}
    }

    def "test listing all plugins"() {
        when:
            List pluginsList = mauroPluginService.listPlugins()

        then:
            pluginsList.size() == 2
            pluginsList.find {
                it.name == "JsonDataModelImporterPlugin" &&
                it.displayName == "JSON DataModel Importer" &&
                it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "JsonTerminologyImporterPlugin" &&
                it.displayName == "JSON Terminology Importer" &&
                it.version == "4.0.0"
            }

        when:
            List importersList = mauroPluginService.listPlugins(ModelImporterPlugin)
        then:
            importersList == pluginsList
    }

    def "test find plugin by name"() {
        when:
        MauroPlugin plugin = mauroPluginService.getPlugin("uk.ac.ox.softeng.mauro.plugin.importer.json","JsonDataModelImporterPlugin")

        then:
        plugin.displayName == "JSON DataModel Importer"
        plugin.version == "4.0.0"


        when:
            plugin = mauroPluginService.getPlugin("uk.ac.ox.softeng.mauro.plugin.importer.json","JsonDataModelImporterPlugin", "4.0.0")

        then:
            plugin.displayName == "JSON DataModel Importer"
            plugin.version == "4.0.0"

        when:
            plugin = mauroPluginService.getPlugin("uk.ac.ox.softeng.mauro.plugin.importer.json","JsonDataModelImporterPlugin", "3.0.0")

        then:
            !plugin
    }


}