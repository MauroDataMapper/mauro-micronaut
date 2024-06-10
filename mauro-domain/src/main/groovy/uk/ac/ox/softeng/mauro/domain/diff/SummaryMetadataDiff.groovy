package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadataType

@CompileStatic
class SummaryMetadataDiff extends CollectionDiff {

    String label
    SummaryMetadataType summaryMetadataType;

    SummaryMetadataDiff(UUID id, SummaryMetadataType summaryMetadataType, String label) {
        super(id)
        this.summaryMetadataType = summaryMetadataType
        this.label = label
    }

}
