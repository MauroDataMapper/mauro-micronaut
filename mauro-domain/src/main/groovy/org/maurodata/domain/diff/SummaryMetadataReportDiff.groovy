package org.maurodata.domain.diff

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable

import java.time.Instant

@CompileStatic
class SummaryMetadataReportDiff extends CollectionDiff {
    @Nullable
    Instant reportDate
    @NonNull
    String reportValue

    SummaryMetadataReportDiff(UUID id, @Nullable Instant reportDate, @NonNull String reportValue, String diffIdentifier) {
        super(id, diffIdentifier)
        this.reportDate = reportDate
        this.reportValue = reportValue
    }

}
