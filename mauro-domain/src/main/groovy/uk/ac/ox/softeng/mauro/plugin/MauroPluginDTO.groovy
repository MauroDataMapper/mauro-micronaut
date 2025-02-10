package uk.ac.ox.softeng.mauro.plugin

import uk.ac.ox.softeng.mauro.plugin.exporter.ModelExporterPlugin
import uk.ac.ox.softeng.mauro.plugin.importer.ModelImporterPlugin
import uk.ac.ox.softeng.mauro.profile.Profile

class MauroPluginDTO {
    String name
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

    // Profile
    String metadataNamespace
    List<String> profileApplicableForDomains

    static MauroPluginDTO fromPlugin(MauroPlugin mauroPlugin) {
        new MauroPluginDTO().tap {
            name = mauroPlugin.name
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
            if(mauroPlugin instanceof Profile) {
                metadataNamespace = mauroPlugin.metadataNamespace
                profileApplicableForDomains = mauroPlugin.profileApplicableForDomains
            }
        }
    }

}
