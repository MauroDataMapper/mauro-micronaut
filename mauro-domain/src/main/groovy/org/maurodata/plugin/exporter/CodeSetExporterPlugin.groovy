package org.maurodata.plugin.exporter


import org.maurodata.domain.terminology.CodeSet

trait CodeSetExporterPlugin extends ModelExporterPlugin<CodeSet> {

    @Override
    Class<CodeSet> getHandlesModelType() {
        CodeSet
    }
}
