package org.maurodata.persistence.facet

import org.maurodata.persistence.model.ItemRepository

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.Query
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import org.maurodata.domain.facet.SummaryMetadataReport

@CompileStatic
@JdbcRepository(dialect = Dialect.POSTGRES)
abstract class SummaryMetadataReportRepository implements ItemRepository<SummaryMetadataReport> {

    @Query(''' select * from core.summary_metadata_report smr where smr.summary_metadata_id = :summaryMetadataId ''')
    @Nullable
    abstract List<SummaryMetadataReport> findAllBySummaryMetadataId(@NonNull UUID summaryMetadataId)


    abstract List<SummaryMetadataReport> readAllBySummaryMetadataIdIn(List<UUID> summaryMetadataIds)


    @Override
    Class getDomainClass() {
        SummaryMetadataReport
    }

    Boolean handlesPathPrefix(final String pathPrefix) {
        'smr'.equalsIgnoreCase(pathPrefix)
    }
}
