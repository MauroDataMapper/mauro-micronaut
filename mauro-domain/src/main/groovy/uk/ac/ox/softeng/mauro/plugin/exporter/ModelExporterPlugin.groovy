package uk.ac.ox.softeng.mauro.plugin.exporter

import uk.ac.ox.softeng.mauro.domain.model.Model

trait ModelExporterPlugin<D extends Model> {

    abstract ByteArrayOutputStream exportDomain(D model, Map<String, Object> parameters)

    abstract ByteArrayOutputStream exportDomains(List<D> models, Map<String, Object> parameters)

    abstract Boolean canExportMultipleDomains()

    abstract String getFileExtension()

    /**
     * MIME type produced by ExporterProviderService
     * @return MIME type
     */
    abstract String getContentType()


}
