package uk.ac.ox.softeng.mauro.plugin.importer

import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j

@Slf4j
trait ModelImporterPlugin <D extends Model, P extends ImportParameters> extends MauroPlugin {

    abstract List<D> importDomain(P params)

    Boolean getCanImportMultipleDomains() {
        true
    }

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.Importer
    }

    @JsonIgnore
    abstract Class<D> getHandlesModelType()

    String getParamClassType() {
        importParametersClass().toString()
    }

    List<D> importModels(P parameters) {
        List<D> imported = importDomain(parameters)
        imported.each { importedModel ->
            importedModel.setAssociations()
            importedModel.updateCreationProperties()
            log.info '* start updateCreationProperties *'
            importedModel.getAllContents().each {it.updateCreationProperties()}
            log.info '* finish updateCreationProperties *'
        }

        imported
    }

    @Override
    String getProviderType() {
        return "${getHandlesModelType().simpleName}${this.pluginType}"
    }
}
