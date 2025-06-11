package org.maurodata.plugin.exporter

import org.maurodata.domain.terminology.Terminology

trait TerminologyExporterPlugin extends ModelExporterPlugin<Terminology> {

    @Override
    Class<Terminology> getHandlesModelType() {
        Terminology
    }

}
