package org.maurodata.plugin.exporter

import org.maurodata.domain.dataflow.DataFlow
import groovy.transform.CompileStatic

@CompileStatic
trait DataFlowExporterPlugin extends ModelItemExporterPlugin<DataFlow> {

    @Override
    Class<DataFlow> getHandlesModelItemType() {
        DataFlow
    }
}
