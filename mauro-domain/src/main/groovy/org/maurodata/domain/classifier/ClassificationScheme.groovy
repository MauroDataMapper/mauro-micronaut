package org.maurodata.domain.classifier

import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Transient
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.ModelItem

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
    String getDomainType(){
        ClassificationScheme.simpleName
    }

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
            classifier.childClassifiers.each {childClassifier ->
                childClassifier.parentClassifier = classifier
                childClassifier.classificationScheme = this
                childClassifier.parent = childClassifier.parentClassifier
            }
            classifier.parent = classifier.parentClassifier ?: classifier.classificationScheme
            this
        }
    }

    /**
     * DSL builder
     * @param args
     * @param closure
     * @return
     */
    static ClassificationScheme build(
        Map args,
        @DelegatesTo(value = ClassificationScheme, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new ClassificationScheme(args).tap(closure)
    }

    static ClassificationScheme build(
        @DelegatesTo(value = ClassificationScheme, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure

    }

    Classifier classifier(Classifier classifier) {
        this.classifiers.add(classifier)
        classifier.classificationScheme = this
        classifier
    }

    Classifier classifier(@DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        classifier [:], closure
    }

    Classifier classifier(Map args, @DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Classifier classifier1 = Classifier.build(args, closure)
        classifier1.classificationScheme = this
        this.classifiers.add(classifier1)
        classifier1
    }

}