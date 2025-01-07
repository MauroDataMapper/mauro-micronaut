package uk.ac.ox.softeng.mauro.plugin.exporter


import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet

trait CodeSetExporterPlugin extends ModelExporterPlugin<CodeSet> {

    @Override
    Class<CodeSet> getHandlesModelType() {
        CodeSet
    }
}
