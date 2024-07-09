package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import uk.ac.ox.softeng.mauro.domain.datamodel.DataType
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

@CompileStatic
@Introspected
@MappedEntity(value = 'data_type', schema = 'datamodel', alias = 'data_type_')
class DataTypeDTO extends DataType implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = data_type_.id)')
    List<Metadata> metadata = []


    @Override
    String getDomainType() {
        this.dataTypeKind.stringValue
    }


    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report
                                    where summary_metadata_id = summary_metadata.id) summary_metadata_reports
                                    from core.summary_metadata) summary_metadata where multi_facet_aware_item_id = data_type_.id)''')
    List<SummaryMetadata> summaryMetadata = []

    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(annotation)  from (select *,
                                (select json_agg(c) from core.annotation c
                                where c.parent_annotation_id = annotation.id) child_annotations
                                from core.annotation) annotation where
                                                      multi_facet_aware_item_id = data_type_.id
                                                      and parent_annotation_id is null )''')
    List<Annotation> annotations = []

    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(to_jsonb(reference_file) - 'file_contents') from core.reference_file where multi_facet_aware_item_id = data_type_.id)''')
    List<ReferenceFile> referenceFiles = []
}
