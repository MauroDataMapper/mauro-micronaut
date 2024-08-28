package uk.ac.ox.softeng.mauro.domain.classifier

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.transform.AutoClone
import groovy.transform.CompileStatic
import groovy.transform.MapConstructor
import io.micronaut.core.annotation.Introspected
import io.micronaut.core.annotation.Nullable
import io.micronaut.data.annotation.MappedEntity
import io.micronaut.data.annotation.Relation
import jakarta.persistence.Inheritance
import jakarta.persistence.InheritanceType
import jakarta.persistence.Transient
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.diff.*
import uk.ac.ox.softeng.mauro.domain.model.AdministeredItem
import uk.ac.ox.softeng.mauro.domain.model.ModelItem

@CompileStatic
@AutoClone(excludes = ['classificationScheme'])
@Introspected
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@MappedEntity(schema = 'core', value = 'classifier')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Classifier extends ModelItem<ClassificationScheme> implements DiffableItem<Classifier> {

    @JsonIgnore
    ClassificationScheme classificationScheme

    @JsonAlias(['child_classifiers'])
    @Relation(value = Relation.Kind.ONE_TO_MANY, mappedBy = 'parentClassifier')
    List<Classifier> childClassifiers = []

    @Nullable
    @JsonIgnore
    Classifier parentClassifier

    @Override
    @Transient
    @JsonIgnore
    AdministeredItem getParent() {
        parentClassifier?:classificationScheme

    }

    @Override
    @Transient
    @JsonIgnore
    void setParent(AdministeredItem parent) {
        if(parent instanceof Classifier) {
            this.parentClassifier = parent
            this.classificationScheme = parentClassifier.classificationScheme
        } else {
            this.classificationScheme = (ClassificationScheme) parent
        }
    }


    @Override
    @Transient
    @JsonIgnore
    String getPathPrefix() {
        'cl'
    }

    @Override
    @Transient
    @JsonIgnore
    List<Collection<? extends ModelItem<Classifier>>> getAllAssociations() {
        [childClassifiers] as List<Collection<? extends ModelItem<Classifier>>>
    }

    @Transient
    UUID breadcrumbTreeId

    @Override
    @JsonIgnore
    @Transient
    CollectionDiff fromItem() {
        new BaseCollectionDiff(id, label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (!parentClassifier) return this.pathIdentifier
        "${parentClassifier.getDiffIdentifier()}/${this.pathIdentifier}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Classifier> diff(Classifier other) {
        ObjectDiff<Classifier> base = DiffBuilder.objectDiff(Classifier)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString)
        if (!DiffBuilder.isNullOrEmpty(this.classifiers as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.classifiers as Collection<Object>)) {
            base.appendCollection(DiffBuilder.CLASSIFIERS, this.classifiers as Collection<DiffableItem>, other.classifiers as Collection<DiffableItem>)
        }
        base
    }

//    /****
//     * Methods for building a tree-like DSL
//     */
//
//    static Classifier build(
//            Map args,
//            @DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
//        new Classifier(args).tap(closure)
//    }
//
//    static Classifier build(@DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
//        build [:], closure
//    }
//
//    Classifier classifier(Classifier classifier) {
//        this.classifiers.add(classifier)
//        classifier.parentClassifier = this
//        classifier.classificationScheme = this.classificationScheme
//        this.classificationScheme.classifiers.add(classifier)
//        classifier
//    }
//
//    Classifier dataClass(Map args, @DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
//        Classifier dataClass = build(args + [dataModel: this.dataModel], closure)
//        this.dataClasses.add(dataClass)
//        dataClass.parentDataClass = this
//        dataModel.allDataClasses.add(dataClass)
//        dataClass
//    }
//
//    Classifier dataClass(@DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
//        dataClass [:], closure
//    }


}
