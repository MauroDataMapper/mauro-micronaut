package org.maurodata.persistence.datamodel.dto

import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.persistence.model.dto.AdministeredItemDTO

import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity(value = 'data_type', schema = 'datamodel', alias = 'data_type_')
class DataTypeDTO extends DataType implements AdministeredItemDTO {

    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = """(select json_agg(x) from
        (select edit.id,
                edit.title,
                edit.description,
                edit.date_created,
                row_to_json(catalogue_user) as catalogue_user
         from core.edit left join security.catalogue_user
              on security.catalogue_user.id = core.edit.created_by
         where multi_facet_aware_item_id = data_type_.id
         group by edit.id, catalogue_user.id) x)""")
    List<Edit> edits = []

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
    @ColumnTransformer(read = '''(select json_agg(rule) from (select *,
                                    (select json_agg(rule_representation)
                                    from core.rule_representation
                                    where rule_id = rule.id) rule_representations
                                    from core.rule) rule where multi_facet_aware_item_id = data_type_.id)''')
    List<Rule> rules = []

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
    @ColumnTransformer(read = '''(select json_agg( jsonb_build_object('id', reference_file.id, 'file_name', reference_file.file_name, 'file_size', reference_file.file_size,
 'file_type',reference_file.file_type) ) from core.reference_file where multi_facet_aware_item_id = data_type_.id)''')
    List<ReferenceFile> referenceFiles = []


    @Nullable
    @TypeDef(type = io.micronaut.data.model.DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''( select json_agg(classifier)
                from core.classifier
                JOIN core.join_administered_item_to_classifier on join_administered_item_to_classifier.classifier_id = core.classifier.id
                and join_administered_item_to_classifier.catalogue_item_id = data_type_.id)''')
    List<Classifier> classifiers = []

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItems(edits, pathsBeingReferenced)
        ItemReferencerUtils.addItems(metadata, pathsBeingReferenced)
        ItemReferencerUtils.addItems(summaryMetadata, pathsBeingReferenced)
        ItemReferencerUtils.addItems(rules, pathsBeingReferenced)
        ItemReferencerUtils.addItems(annotations, pathsBeingReferenced)
        ItemReferencerUtils.addItems(classifiers, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)

        edits = ItemReferencerUtils.replaceItemsByIdentity(edits, replacements, notReplaced)
        metadata = ItemReferencerUtils.replaceItemsByIdentity(metadata, replacements, notReplaced)
        summaryMetadata = ItemReferencerUtils.replaceItemsByIdentity(summaryMetadata, replacements, notReplaced)
        rules = ItemReferencerUtils.replaceItemsByIdentity(rules, replacements, notReplaced)
        annotations = ItemReferencerUtils.replaceItemsByIdentity(annotations, replacements, notReplaced)
        referenceFiles = ItemReferencerUtils.replaceItemsByIdentity(referenceFiles, replacements, notReplaced)
        classifiers = ItemReferencerUtils.replaceItemsByIdentity(classifiers, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)

        if (into instanceof DataTypeDTO) {
            DataTypeDTO intoDTO = (DataTypeDTO) into

            intoDTO.edits = ItemUtils.copyItems(this.edits, intoDTO.edits)
            intoDTO.metadata = ItemUtils.copyItems(this.metadata, intoDTO.metadata)
            intoDTO.summaryMetadata = ItemUtils.copyItems(this.summaryMetadata, intoDTO.summaryMetadata)
            intoDTO.rules = ItemUtils.copyItems(this.rules, intoDTO.rules)
            intoDTO.annotations = ItemUtils.copyItems(this.annotations, intoDTO.annotations)
            intoDTO.referenceFiles = ItemUtils.copyItems(this.referenceFiles, intoDTO.referenceFiles)
            intoDTO.classifiers = ItemUtils.copyItems(this.classifiers, intoDTO.classifiers)
        } else {
            AdministeredItem intoAM = (AdministeredItem) into

            intoAM.edits = ItemUtils.copyItems(this.edits, intoAM.edits)
            intoAM.metadata = ItemUtils.copyItems(this.metadata, intoAM.metadata)
            intoAM.summaryMetadata = ItemUtils.copyItems(this.summaryMetadata, intoAM.summaryMetadata)
            intoAM.rules = ItemUtils.copyItems(this.rules, intoAM.rules)
            intoAM.annotations = ItemUtils.copyItems(this.annotations, intoAM.annotations)
            intoAM.referenceFiles = ItemUtils.copyItems(this.referenceFiles, intoAM.referenceFiles)
            intoAM.classifiers = ItemUtils.copyItems(this.classifiers, intoAM.classifiers)
        }
    }

    @Override
    Item shallowCopy() {
        DataTypeDTO dtoShallowCopy = new DataTypeDTO()
        this.copyInto(dtoShallowCopy)
        return dtoShallowCopy
    }
}
