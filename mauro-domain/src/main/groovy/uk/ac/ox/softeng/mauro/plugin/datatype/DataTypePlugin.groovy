package uk.ac.ox.softeng.mauro.plugin.datatype

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.util.logging.Slf4j
import uk.ac.ox.softeng.mauro.plugin.MauroPlugin
import uk.ac.ox.softeng.mauro.plugin.PluginType

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
