package org.maurodata.plugin.datatype

import com.fasterxml.jackson.annotation.JsonIgnore
import org.maurodata.domain.datamodel.DataType
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType


trait DefaultDataTypeProviderPlugin extends MauroPlugin {

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.DefaultDataTypeProvider
    }

    @JsonIgnore
    List<DataType> dataTypes

}
