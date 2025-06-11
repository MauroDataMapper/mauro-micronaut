package org.maurodata.plugin.datatype

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

trait DataTypePlugin extends MauroPlugin {

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.DataType
    }

    @JsonIgnore
    List<DefaultDataType> dataTypes

    String getDisplayName() {
        description
    }
}
