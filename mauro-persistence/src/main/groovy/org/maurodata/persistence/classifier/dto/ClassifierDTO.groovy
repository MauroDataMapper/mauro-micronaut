package org.maurodata.persistence.classifier.dto

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonIgnore
import io.micronaut.data.model.DataType
import org.maurodata.domain.facet.Rule

import groovy.transform.CompileStatic
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.MappedProperty
import io.micronaut.data.annotation.TypeDef
import io.micronaut.data.annotation.sql.ColumnTransformer
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.persistence.model.dto.AdministeredItemDTO

import jakarta.persistence.Transient

@CompileStatic
@Introspected
@MappedEntity(value = 'classifier', schema = 'core', alias = 'classifier_')
class ClassifierDTO extends Classifier implements AdministeredItemDTO {


    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '(select json_agg(metadata) from core.metadata where multi_facet_aware_item_id = classifier_.id)')
    // could use @JsonRawValue if we need to speed up binding this from the DB
    List<Metadata> metadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(summary_metadata) from (select *,
                                    (select json_agg(summary_metadata_report)
                                    from core.summary_metadata_report
                                    where summary_metadata_id = summary_metadata.id) summary_metadata_reports
                                    from core.summary_metadata) summary_metadata where multi_facet_aware_item_id = classifier_.id)''')
    List<SummaryMetadata> summaryMetadata = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(rule) from (select *,
                                    (select json_agg(rule_representation)
                                    from core.rule_representation
                                    where rule_id = rule.id) rule_representations
                                    from core.rule) rule where multi_facet_aware_item_id = classifier_.id)''')
    List<Rule> rules = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg(annotation)  from (select *,
                                (select json_agg(c) from core.annotation c
                                where c.parent_annotation_id = annotation.id) child_annotations
                                from core.annotation) annotation where
                                                      multi_facet_aware_item_id = classifier_.id
                                                      and parent_annotation_id is null )''')
    List<Annotation> annotations = []

    @Nullable
    @TypeDef(type = DataType.JSON)
    @MappedProperty
    @ColumnTransformer(read = '''(select json_agg( jsonb_build_object('id', reference_file.id, 'file_name', reference_file.file_name, 'file_size', reference_file.file_size,
 'file_type',reference_file.file_type) ) from core.reference_file where multi_facet_aware_item_id = classifier_.id)''')
    List<ReferenceFile> referenceFiles = []

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        // ItemReferencerUtils.addItems(edits, pathsBeingReferenced)
        ItemReferencerUtils.addItems(metadata, pathsBeingReferenced)
        ItemReferencerUtils.addItems(summaryMetadata, pathsBeingReferenced)
        ItemReferencerUtils.addItems(rules, pathsBeingReferenced)
        ItemReferencerUtils.addItems(annotations, pathsBeingReferenced)
        ItemReferencerUtils.addItems(referenceFiles, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, notReplaced)

        // edits = ItemReferencerUtils.replaceItems(edits, replacements,notReplaced)
        metadata = ItemReferencerUtils.replaceItemsByIdentity(metadata, replacements, notReplaced)
        summaryMetadata = ItemReferencerUtils.replaceItemsByIdentity(summaryMetadata, replacements, notReplaced)
        rules = ItemReferencerUtils.replaceItemsByIdentity(rules, replacements, notReplaced)
        annotations = ItemReferencerUtils.replaceItemsByIdentity(annotations, replacements, notReplaced)
        referenceFiles = ItemReferencerUtils.replaceItemsByIdentity(referenceFiles, replacements, notReplaced)
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ClassifierDTO intoDTO = (ClassifierDTO) into

        // intoDTO.edits += this.edits
        intoDTO.metadata = ItemUtils.copyItems(this.metadata, intoDTO.metadata)
        intoDTO.summaryMetadata = ItemUtils.copyItems(this.summaryMetadata, intoDTO.summaryMetadata)
        intoDTO.rules = ItemUtils.copyItems(this.rules, intoDTO.rules)
        intoDTO.annotations = ItemUtils.copyItems(this.annotations, intoDTO.annotations)
        intoDTO.referenceFiles = ItemUtils.copyItems(this.referenceFiles, intoDTO.referenceFiles)
    }

    @Override
    Item shallowCopy() {
        ClassifierDTO dtoShallowCopy = new ClassifierDTO()
        this.copyInto(dtoShallowCopy)
        return dtoShallowCopy
    }
}
