package org.maurodata.domain.classifier

import com.fasterxml.jackson.annotation.JsonAlias
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
    @JsonAlias("classifiers") // for importing models exported from the Grails implementation
    List<Classifier> csClassifiers = []

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
        [csClassifiers] as List<Collection<? extends ModelItem<ClassificationScheme>>>
    }

    @Override
    ClassificationScheme clone() {
        ClassificationScheme cloned = (ClassificationScheme) super.clone()
        List<Classifier> clonedClassifiers = csClassifiers.collect {it ->
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
        cloned.csClassifiers = clonedClassifiers
        cloned
    }

    @Transient
    @JsonIgnore
    @Override
    void setAssociations() {
        this.csClassifiers.collect {classifier ->
            classifier.classificationScheme = this
            classifier.childClassifiers.each {childClassifier ->
                childClassifier.parentClassifier = classifier
                childClassifier.classificationScheme = this
                childClassifier.parent = childClassifier.parentClassifier
            }
            classifier.parent = classifier.parentClassifier ?: classifier.classificationScheme
            classifier
        }
        this
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if(folder!=null) {
            return "${folder.getDiffIdentifier()}|${getPathNodeString()}"
        }
        return "${getPathNodeString()}"
    }
}