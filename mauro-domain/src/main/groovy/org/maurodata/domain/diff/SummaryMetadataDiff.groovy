package org.maurodata.domain.diff

import org.maurodata.domain.facet.SummaryMetadataReport
import groovy.transform.CompileStatic
import org.maurodata.domain.facet.SummaryMetadataType

@CompileStatic
class SummaryMetadataDiff extends CollectionDiff {

    String label
    SummaryMetadataType summaryMetadataType

    Collection<SummaryMetadataReport> summaryMetadataReports

    SummaryMetadataDiff(UUID id, SummaryMetadataType summaryMetadataType, String label, Collection<SummaryMetadataReport> summaryMetadataReports, String diffIdentifier) {
        super(id,diffIdentifier)
        this.summaryMetadataType = summaryMetadataType
        this.label = label
        this.summaryMetadataReports = summaryMetadataReports
    }

}
