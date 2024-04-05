package uk.ac.ox.softeng.mauro.persistence.terminology.dto

import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.domain.terminology.TermRelationship
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType

@CompileStatic
@Introspected
@MappedEntity(value = 'term_relationship', schema = 'terminology', alias = 'term_relationship_')
class TermRelationshipDTO extends TermRelationship implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = term_relationship_.id)')
    // could use @JsonRawValue if we need to speed up binding this from the DB
    List<Metadata> metadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report
                                    where summary_metadata_id = summary_metadata.id) summary_metadata_reports
                                    from core.summary_metadata) summary_metadata where multi_facet_aware_item_id = term_relationship_.id)''')
    List<SummaryMetadata> summaryMetadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(annotation)  from (select *,
                                (select json_agg(c) from core.annotation c
                                where c.parent_annotation_id = annotation.id) child_annotations
                                from core.annotation) annotation where
                                                      multi_facet_aware_item_id = term_relationship_.id
                                                      and parent_annotation_id is null )''')
    List<Annotation> annotations = []
}
