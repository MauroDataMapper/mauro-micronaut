package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.*
import uk.ac.ox.softeng.mauro.domain.diff.AnnotationDiff
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(value = 'annotation', schema = 'core', alias = 'annotation_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class Annotation extends Facet implements DiffableItem<Annotation>{

    @JsonAlias(['parent_annotation_id'])
    UUID parentAnnotationId

    String description
    String label

    @JsonIgnore
    @Transient
    CatalogueUser createdByUser

    @JsonAlias(['child_annotations'])
    @Relation(Relation.Kind.ONE_TO_MANY)
    List<Annotation> childAnnotations

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new AnnotationDiff(id, label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
       label
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Annotation> diff(Annotation other) {
        //todo
        return null
    }
}
