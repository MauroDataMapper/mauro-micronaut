package uk.ac.ox.softeng.mauro.persistence.datamodel.dto

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import io.micronaut.data.model.DataType
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.Annotation
import uk.ac.ox.softeng.mauro.domain.facet.Edit
import uk.ac.ox.softeng.mauro.domain.facet.Rule
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.facet.ReferenceFile
import uk.ac.ox.softeng.mauro.domain.facet.SummaryMetadata
import uk.ac.ox.softeng.mauro.persistence.model.dto.AdministeredItemDTO

@CompileStatic
@Introspected
@MappedEntity(value = 'data_model', schema = 'datamodel', alias = 'data_model_')
class DataModelDTO extends DataModel implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = """(select json_agg(x) from
        (select edit.id,
                edit.title,
                edit.description,
                edit.date_created,
                row_to_json(catalogue_user) as catalogue_user
         from core.edit left join security.catalogue_user
              on security.catalogue_user.id = core.edit.created_by
         where multi_facet_aware_item_id = data_model_.id
         group by edit.id, catalogue_user.id) x)""")
    List<Edit> edits = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = data_model_.id)')
    List<Metadata> metadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report
                                    where summary_metadata_id = summary_metadata.id) summary_metadata_reports
                                    from core.summary_metadata) summary_metadata where multi_facet_aware_item_id = data_model_.id)''')
    List<SummaryMetadata> summaryMetadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(rule) from (select *,
                                    (select json_agg(rule_representation)
                                    from core.rule_representation
                                    where rule_id = rule.id) rule_representations
                                    from core.rule) rule where multi_facet_aware_item_id = data_model_.id)''')
    List<Rule> rules = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(annotation)  from (select *,
                                (select json_agg(c) from core.annotation c
                                where c.parent_annotation_id = annotation.id) child_annotations
                                from core.annotation) annotation where
                                                      multi_facet_aware_item_id = data_model_.id
                                                      and parent_annotation_id is null )''')
    List<Annotation> annotations = []

    //*** Warning - this suppresses the referenceFile.fileContents field
    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg( jsonb_build_object('id', reference_file.id, 'file_name', reference_file.file_name, 'file_size', reference_file.file_size, 'file_type',reference_file.file_type) ) from core.reference_file where multi_facet_aware_item_id = data_model_.id)''')
    List<ReferenceFile> referenceFiles = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''( select json_agg(classifier)
                from core.classifier
                JOIN core.join_administered_item_to_classifier on join_administered_item_to_classifier.classifier_id = core.classifier.id
                and join_administered_item_to_classifier.catalogue_item_id = data_model_.id)''')
    List<Classifier> classifiers = []
}
