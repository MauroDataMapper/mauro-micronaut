package uk.ac.ox.softeng.mauro.plugin.exporter

import uk.ac.ox.softeng.mauro.domain.terminology.Terminology

trait TerminologyExporterPlugin extends ModelExporterPlugin<Terminology> {

    @Override
    Class<Terminology> getHandlesModelType() {
        Terminology
    }

}
