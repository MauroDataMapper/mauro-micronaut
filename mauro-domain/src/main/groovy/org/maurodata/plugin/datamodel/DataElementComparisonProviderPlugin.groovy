package org.maurodata.plugin.datamodel

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import org.maurodata.domain.comparison.ComparisonContext
import org.maurodata.domain.comparison.ComparisonResult
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.plugin.MauroPlugin
import org.maurodata.plugin.PluginType

@CompileStatic
trait DataElementComparisonProviderPlugin extends MauroPlugin {

    @JsonIgnore
    PluginType getPluginType() {
        PluginType.DataElementComparisonProvider
    }

    abstract List<ComparisonResult> compare(DataElement left, DataElement right, ComparisonContext context)
}
