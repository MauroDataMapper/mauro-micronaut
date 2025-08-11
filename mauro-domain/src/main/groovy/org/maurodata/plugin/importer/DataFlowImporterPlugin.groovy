package org.maurodata.plugin.importer

import org.maurodata.domain.dataflow.DataFlow

trait DataFlowImporterPlugin<P extends ImportParameters> extends ModelItemImporterPlugin<DataFlow, P> {

    abstract Boolean handlesContentType(String contentType)

    Boolean canFederate() { true }

    abstract Class<P> importParametersClass()

    @Override
    Class<DataFlow> getHandlesModelItemType() {
        DataFlow
    }




}
