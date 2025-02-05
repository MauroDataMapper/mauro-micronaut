package uk.ac.ox.softeng.mauro.plugin

import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin

class MauroPluginDTO {
    String version
    String displayName
    String description
    String namespace
    String providerType

    // EmailPlugin
    Boolean enabled

    // ModelImporterPlugin
    Boolean canImportMultipleDomains
    String importParametersClass

    // ModelExporterPlugin
    Boolean canExportMultipleDomains
    String fileExtension
    String handlesModelType


    static MauroPluginDTO fromPlugin(MauroPlugin mauroPlugin) {
        new MauroPluginDTO().tap {
            version = mauroPlugin.version
            displayName = mauroPlugin.displayName
            description = mauroPlugin.description
            namespace = mauroPlugin.namespace
            providerType = mauroPlugin.providerType
            if(mauroPlugin instanceof EmailPlugin) {
                enabled = mauroPlugin.enabled
            }
            if(mauroPlugin instanceof ModelImporterPlugin) {
                canImportMultipleDomains = mauroPlugin.canImportMultipleDomains
                importParametersClass = mauroPlugin.importParametersClass().name

            }
            if(mauroPlugin instanceof ModelExporterPlugin) {
                canExportMultipleDomains = mauroPlugin.canExportMultipleDomains
                fileExtension = mauroPlugin.fileExtension
                handlesModelType = mauroPlugin.handlesModelType.name
            }
        }
    }

}
