package org.maurodata.plugin.exporter

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.maurodata.domain.model.ModelItem
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

@CompileStatic
@Slf4j
trait ModelItemExporterPlugin<D extends ModelItem> extends ExporterPlugin{

    abstract byte[] exportModelItem(D modelItem)

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
    abstract Class<D> getHandlesModelItemType()

    abstract String getFileName(D modelItem)

    abstract byte[] exportModelItems(Collection<D> modelItems)

    @Override
    String getProviderType() {
        return "${getHandlesModelItemType().simpleName}${this.pluginType}"
    }
}
