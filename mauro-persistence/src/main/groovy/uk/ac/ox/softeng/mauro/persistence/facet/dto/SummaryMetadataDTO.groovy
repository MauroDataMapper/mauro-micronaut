package uk.ac.ox.softeng.mauro.persistence.facet.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.model.SummaryMetadataReport

@CompileStatic
@Introspected
@MappedEntity(value = 'summary_metadata', schema = 'core', alias = 'summary_metadata_')
class SummaryMetadataDTO extends SummaryMetadata {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report where summary_metadata_id = summary_metadata_.id)''')
    List<SummaryMetadataReport> summaryMetadataReport = []

}
