package org.maurodata.domain.facet

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemUtils

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Transient
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(value = 'annotation', schema = 'core', alias = 'annotation_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class Annotation extends Facet implements DiffableItem<Annotation> {

    @JsonAlias(['parent_annotation_id'])
    UUID parentAnnotationId

    String description
    String label

    @Transient
    @JsonProperty('createdByUser')
    CatalogueUser createdByUser

    @JsonAlias(['child_annotations'])
    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Annotation> childAnnotations = []


    CatalogueUser getCreatedByUser() {
        createdByUser ? createdByUser : catalogueUser
    }

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, getDiffIdentifier(), label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (multiFacetAwareItem != null) {
            return "${multiFacetAwareItem.getDiffIdentifier()}|${this.pathNodeString}"
        }
        return "${this.pathNodeString}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Annotation> diff(Annotation other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Annotation> base = DiffBuilder.objectDiff(Annotation)
            .leftHandSide(id?.toString(), this)
            .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        if (!DiffBuilder.isNull(this.childAnnotations) || !DiffBuilder.isNull(other.childAnnotations)) {
            base.appendCollection(DiffBuilder.CHILD_ANNOTATIONS, this.childAnnotations as Collection<DiffableItem>, other.childAnnotations as Collection<DiffableItem>,
                                  lhsPathRoot, rhsPathRoot)
        }
        base
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathPrefix() {
        'ann'
    }

    @Transient
    @JsonIgnore
    @Override
    String getPathIdentifier() {
        label
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Annotation intoAnnotation = (Annotation) into
        intoAnnotation.parentAnnotationId = ItemUtils.copyItem(this.parentAnnotationId, intoAnnotation.parentAnnotationId)
        intoAnnotation.description = ItemUtils.copyItem(this.description, intoAnnotation.description)
        intoAnnotation.label = ItemUtils.copyItem(this.label, intoAnnotation.label)
        intoAnnotation.createdByUser = ItemUtils.copyItem(this.createdByUser, intoAnnotation.createdByUser)
        intoAnnotation.childAnnotations = ItemUtils.copyItems(this.childAnnotations, intoAnnotation.childAnnotations)
    }

    @Override
    Item shallowCopy() {
        Annotation annotationShallowCopy = new Annotation()
        this.copyInto(annotationShallowCopy)
        return annotationShallowCopy
    }
}
