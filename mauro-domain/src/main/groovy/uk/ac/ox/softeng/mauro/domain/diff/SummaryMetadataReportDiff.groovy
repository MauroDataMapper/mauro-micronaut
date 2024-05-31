package uk.ac.ox.softeng.mauro.domain.diff

import groovy.transform.CompileStatic

import java.time.Instant

@CompileStatic
class SummaryMetadataReportDiff extends CollectionDiff {

    Instant reportDate

    SummaryMetadataReportDiff(UUID id, Instant reportDate) {
        super(id)
        this.reportDate = reportDate
    }


}
