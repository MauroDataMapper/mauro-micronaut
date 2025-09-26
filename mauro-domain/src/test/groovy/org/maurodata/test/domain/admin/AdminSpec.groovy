package org.maurodata.test.domain.admin

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.MauroPluginService
import org.maurodata.plugin.PluginType
import org.maurodata.plugin.exporter.ExporterPlugin
import org.maurodata.plugin.importer.ImporterPlugin
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
            modulesList.find {it["name"] == "JsonDataModelExporterPlugin" && it["version"] == "4.0.0"}
            modulesList.find {it["name"] == "java.base" && it["version"] == javaVersion}
    }

    def "test listing all plugins"() {
        when:
            List pluginsList = mauroPluginService.listPlugins()

        then:
            pluginsList.size() == 14
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
            pluginsList.find {
                it.name == "JsonFolderImporterPlugin" &&
                        it.displayName == "JSON Folder Importer" &&
                        it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "JsonDataModelExporterPlugin" &&
                        it.displayName == "JSON DataModel Exporter" &&
                        it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "JsonTerminologyExporterPlugin" &&
                        it.displayName == "JSON Terminology Exporter" &&
                        it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "JsonFolderExporterPlugin" &&
                        it.displayName == "JSON Folder Exporter" &&
                        it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "ProfileSpecificationProfile" &&
                        it.displayName == "Profile Specification Profile" &&
                        it.version == "1.0.0"
            }
            pluginsList.find {
                it.name == "ProfileSpecificationFieldProfile" &&
                        it.displayName == "Profile Specification Field Profile" &&
                        it.version == "1.0.0"
            }
            pluginsList.find {
                it.name == "JsonCodeSetExporterPlugin" &&
                it.displayName == "JSON CodeSet Exporter" &&
                it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == "JsonCodeSetImporterPlugin" &&
                it.displayName == "JSON CodeSet Importer" &&
                it.version == "4.0.0"
            }
            pluginsList.find {
                it.name == 'JsonDataFlowImporterPlugin' &&
                it.displayName == "JSON DataFlow Importer" &&
                it.version == "4.0.0"
            }
        pluginsList.find {
            it.name == 'JsonDataFlowExporterPlugin' &&
            it.displayName == "JSON DataFlow Exporter" &&
            it.version == "4.0.0"
        }
        pluginsList.find {
            it.name == 'JsonDataFlowImporterPlugin' &&
            it.displayName == "JSON DataFlow Importer" &&
            it.version == "4.0.0"
        }
            pluginsList.find {
                it.name == "MauroDataTypeProviderService" &&
                it.displayName == "Basic Default DataTypes" &&
                it.version == "1.0.0"
            }
            pluginsList.find {
                it.name == "ProfileSpecificationDataTypeProvider" &&
                it.displayName == "Profile Specification DataTypes" &&
                it.version == "1.0.0"
            }

        when:
            List importersList = mauroPluginService.listPlugins(ImporterPlugin)
        then:
            importersList == pluginsList.findAll{it.pluginType == PluginType.Importer}

        when:
            List exportersList = mauroPluginService.listPlugins(ExporterPlugin)
        then:
            exportersList == pluginsList.findAll{it.pluginType == PluginType.Exporter}
    }

    def "test find plugin by name"() {
        when:
        MauroPlugin plugin = mauroPluginService.getPlugin("org.maurodata.plugin.importer.json","JsonDataModelImporterPlugin")

        then:
        plugin.displayName == "JSON DataModel Importer"
        plugin.version == "4.0.0"


        when:
            plugin = mauroPluginService.getPlugin("org.maurodata.plugin.importer.json","JsonDataModelImporterPlugin", "4.0.0")

        then:
            plugin.displayName == "JSON DataModel Importer"
            plugin.version == "4.0.0"

        when:
            plugin = mauroPluginService.getPlugin("org.maurodata.plugin.importer.json","JsonDataModelImporterPlugin", "3.0.0")

        then:
            !plugin
    }


}