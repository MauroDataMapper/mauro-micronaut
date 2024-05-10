package uk.ac.ox.softeng.mauro.domain.facet

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.data.annotation.Index
import io.micronaut.data.annotation.Indexes
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import io.micronaut.data.annotation.Transient
import uk.ac.ox.softeng.mauro.domain.security.CatalogueUser

@CompileStatic
@MappedEntity(value = 'annotation', schema = 'core', alias = 'annotation_')
@AutoClone
@Indexes([@Index(columns = ['multi_facet_aware_item_id'])])
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Annotation extends Facet {

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

    Annotation() {}

    static Annotation build(
        Map args,
        @DelegatesTo(value = Annotation, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        new Annotation(args).tap(closure)
    }

    static Annotation build(@DelegatesTo(value = Annotation, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        build [:], closure
    }

    String description(String description) {
        this.description = description
        this.description
    }

    String label(String label) {
        this.label = label
        this.label
    }

    Annotation annotation(Map args, @DelegatesTo(value = Annotation, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        Annotation annotation = build(args + [multiFacetAwareItem: this.multiFacetAwareItem], closure)
        this.childAnnotations.add(annotation)
        annotation.multiFacetAwareItem = this.multiFacetAwareItem
        annotation
    }

    Annotation annotation(@DelegatesTo(value = Annotation, strategy = Closure.DELEGATE_FIRST) Closure closure = { }) {
        annotation [:], closure
    }

}
