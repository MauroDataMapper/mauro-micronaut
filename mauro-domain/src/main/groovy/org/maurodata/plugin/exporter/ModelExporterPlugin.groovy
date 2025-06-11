package org.maurodata.plugin.exporter

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j
import org.maurodata.domain.model.Model
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

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
