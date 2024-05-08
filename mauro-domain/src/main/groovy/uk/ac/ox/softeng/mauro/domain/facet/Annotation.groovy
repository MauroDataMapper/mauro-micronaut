package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Transient
import uk.ac.ox.softeng.mauro.domain.diff.AnnotationDiff
import uk.ac.ox.softeng.mauro.domain.diff.CollectionDiff
import uk.ac.ox.softeng.mauro.domain.diff.DiffableItem
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(value = 'annotation', schema = 'core', alias = 'annotation_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
class Annotation extends Facet implements DiffableItem{

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
    CollectionDiff fromItem() {
        println("Annotation: fromItem. id : $id")
        new AnnotationDiff(id, label)
    }
}
