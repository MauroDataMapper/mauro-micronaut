package org.maurodata.plugin.exporter


import org.maurodata.domain.terminology.CodeSet

import groovy.transform.CompileStatic

@CompileStatic
trait CodeSetExporterPlugin extends ModelExporterPlugin<CodeSet> {

    @Override
    Class<CodeSet> getHandlesModelType() {
        CodeSet
    }
}
