package org.maurodata.plugin.exporter

import org.maurodata.domain.terminology.Terminology

import groovy.transform.CompileStatic

@CompileStatic
trait TerminologyExporterPlugin extends ModelExporterPlugin<Terminology> {

    @Override
    Class<Terminology> getHandlesModelType() {
        Terminology
    }

}
