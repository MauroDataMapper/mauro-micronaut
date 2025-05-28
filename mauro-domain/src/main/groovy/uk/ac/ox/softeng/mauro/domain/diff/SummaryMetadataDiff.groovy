package uk.ac.ox.softeng.mauro.domain.diff

import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataReport
import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType

@CompileStatic
class SummaryMetadataDiff extends CollectionDiff {

    String label
    SummaryMetadataType summaryMetadataType

    Collection<SummaryMetadataReport> summaryMetadataReports

    SummaryMetadataDiff(UUID id, SummaryMetadataType summaryMetadataType, String label, Collection<SummaryMetadataReport> summaryMetadataReports) {
        super(id)
        this.summaryMetadataType = summaryMetadataType
        this.label = label
        this.summaryMetadataReports = summaryMetadataReports
    }

}
