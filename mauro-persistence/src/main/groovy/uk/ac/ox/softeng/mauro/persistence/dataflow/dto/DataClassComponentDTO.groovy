package uk.ac.ox.softeng.mauro.persistence.dataflow.dto

import uk.ac.ox.softeng.mauro.domain.facet.Rule

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.dataflow.DataClassComponent
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

@CompileStatic
@Introspected
@MappedEntity(value = 'data_class_component', schema = 'dataflow', alias = 'data_class_component_')
class DataClassComponentDTO extends DataClassComponent implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = data_class_component_.id)')
    List<Metadata> metadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report
                                    where summary_metadata_id = summary_metadata.id) summary_metadata_reports
                                    from core.summary_metadata) summary_metadata where multi_facet_aware_item_id = data_class_component_.id)''')
    List<SummaryMetadata> summaryMetadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(rule) from (select *,
                                    (select json_agg(rule_representation)
                                    from core.rule_representation
                                    where rule_id = rule.id) rule_representations
                                    from core.rule) rule where multi_facet_aware_item_id = data_class_component_.id)''')
    List<Rule> rules = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(annotation)  from (select *,
                                (select json_agg(c) from core.annotation c
                                where c.parent_annotation_id = annotation.id) child_annotations
                                from core.annotation) annotation where
                                                      multi_facet_aware_item_id = data_class_component_.id
                                                      and parent_annotation_id is null )''')
    List<Annotation> annotations = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''( select json_agg(classifier)
                from core.classifier
                JOIN core.join_administered_item_to_classifier on join_administered_item_to_classifier.classifier_id = core.classifier.id
                and join_administered_item_to_classifier.catalogue_item_id = data_class_component_.id)''')
    List<Classifier> classifiers = []
}
