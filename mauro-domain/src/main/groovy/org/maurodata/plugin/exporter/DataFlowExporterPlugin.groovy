package org.maurodata.plugin.exporter

import org.maurodata.domain.dataflow.DataFlow

trait DataFlowExporterPlugin extends ModelItemExporterPlugin<DataFlow> {

    @Override
    Class<DataFlow> getHandlesModelItemType() {
        DataFlow
    }
}
