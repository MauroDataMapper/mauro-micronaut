package uk.ac.ox.softeng.mauro.plugin.exporter

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

@Slf4j
trait ModelExporterPlugin<D extends Model> extends MauroPlugin {

    abstract byte[] exportModel(D model)

    Boolean getCanExportMultipleDomains() {
        false
    }

    abstract String getFileExtension()

    abstract String getContentType()

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.Exporter
    }

    @JsonIgnore
    abstract Class<D> getHandlesModelType()

    abstract String getFileName(D model)

    abstract byte[] exportModels(Collection<D> models)

    @Override
    String getProviderType() {
        return "${getHandlesModelType().simpleName}${this.pluginType}"
    }
}
