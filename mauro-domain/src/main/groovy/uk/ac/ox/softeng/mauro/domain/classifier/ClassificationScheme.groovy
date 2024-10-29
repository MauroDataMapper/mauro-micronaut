package uk.ac.ox.softeng.mauro.domain.classifier


import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

/**
 *
 */
@CompileStatic
@AutoClone
@Introspected
@MappedEntity(schema = 'core')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class ClassificationScheme extends Model {

    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'classificationScheme')
    List<Classifier> classifiers = []

    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'csc'
    }
    @Transient
    UUID breadcrumbTreeId

    @Override
    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<ClassificationScheme>>> getAllAssociations() {
        [classifiers] as List<Collection<? extends ModelItem<ClassificationScheme>>>
    }

    @Override
    ClassificationScheme clone() {
        ClassificationScheme cloned = (ClassificationScheme) super.clone()
        List<Classifier> clonedClassifiers = classifiers.collect { it ->
            it.clone().tap { clonedClassifier ->
                clonedClassifier.classificationScheme = cloned
                List<Classifier> clonedChildClassifiers = clonedClassifier.childClassifiers.collect { childClassifier ->
                    childClassifier.clone().tap {
                        it.parentClassifier = clonedClassifier
                        it.classificationScheme = cloned
                    }
                }
                clonedClassifier.childClassifiers = clonedChildClassifiers
            }
        }
        cloned.classifiers = clonedClassifiers
        cloned
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        classifiers.each { classifier ->
            classifier.classificationScheme = this
            classifier.childClassifiers.each { childClassifier ->
                childClassifier.parentClassifier = classifier
                childClassifier.classificationScheme = this
                childClassifier.parent = childClassifier.parentClassifier
            }
            classifier.parent = classifier.parentClassifier ?: classifier.classificationScheme
            this
        }
    }

}