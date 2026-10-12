package org.maurodata.plugin.datatype

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

@CompileStatic
trait DataTypeComparisonProviderPlugin extends MauroPlugin {

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.DataTypeComparisonProvider
    }

    abstract List<ComparisonResult> compare(DataType left, DataType right, ComparisonContext context)
}
