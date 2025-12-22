package org.maurodata.domain.classifier

import org.maurodata.domain.model.Item
import org.maurodata.domain.model.ItemReference
import org.maurodata.domain.model.ItemReferencer
import org.maurodata.domain.model.ItemReferencerUtils
import org.maurodata.domain.model.ItemUtils

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
import org.maurodata.domain.diff.BaseCollectionDiff
import org.maurodata.domain.diff.CollectionDiff
import org.maurodata.domain.diff.DiffBuilder
import org.maurodata.domain.diff.DiffableItem
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.ModelItem

@CompileStatic
@AutoClone(excludes = ['classificationScheme'])
@Introspected
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@MappedEntity(schema = 'core', value = 'classifier')
@MapConstructor(includeSuperFields = true, includeSuperProperties = true, noArg = true)
class Classifier extends ModelItem<ClassificationScheme> implements DiffableItem<Classifier>, ItemReferencer {

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
        new BaseCollectionDiff(id, getDiffIdentifier(), label)
    }

    @Override
    @JsonIgnore
    @Transient
    String getDiffIdentifier() {
        if (parentClassifier != null) {return "${parentClassifier.getDiffIdentifier()}|${getPathNodeString()}"}
        if (classificationScheme != null) {return "${classificationScheme.getDiffIdentifier()}|${getPathNodeString()}"}
        return "${getPathNodeString()}"
    }

    @Override
    @JsonIgnore
    @Transient
    ObjectDiff<Classifier> diff(Classifier other, String lhsPathRoot, String rhsPathRoot) {
        ObjectDiff<Classifier> base = DiffBuilder.objectDiff(Classifier)
                .leftHandSide(id?.toString(), this)
                .rightHandSide(other.id?.toString(), other)
        base.label = this.label
        base.appendString(DiffBuilder.DESCRIPTION, this.description, other.description, this, other)
        base.appendString(DiffBuilder.ALIASES_STRING, this.aliasesString, other.aliasesString, this, other)
        if (!DiffBuilder.isNullOrEmpty(this.classifiers as Collection<Object>) || !DiffBuilder.isNullOrEmpty(other.classifiers as Collection<Object>)) {
            base.appendCollection(DiffBuilder.CLASSIFIERS, this.classifiers as Collection<DiffableItem>, other.classifiers as Collection<DiffableItem>, lhsPathRoot, rhsPathRoot)
        }
        base
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        Classifier intoClassifier = (Classifier) into
        intoClassifier.classificationScheme = ItemUtils.copyItem(this.classificationScheme, intoClassifier.classificationScheme)
        intoClassifier.childClassifiers = ItemUtils.copyItems(this.childClassifiers, intoClassifier.childClassifiers)
        intoClassifier.parentClassifier = ItemUtils.copyItem(this.parentClassifier, intoClassifier.parentClassifier)
        intoClassifier.breadcrumbTreeId = ItemUtils.copyItem(this.breadcrumbTreeId, intoClassifier.breadcrumbTreeId)
    }

    @Override
    Item shallowCopy() {
        Classifier classifierShallowCopy = new Classifier()
        this.copyInto(classifierShallowCopy)
        return classifierShallowCopy
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(classificationScheme, pathsBeingReferenced)
        ItemReferencerUtils.addItems(childClassifiers, pathsBeingReferenced)
        ItemReferencerUtils.addItem(parentClassifier, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        classificationScheme = ItemReferencerUtils.replaceItemByIdentity(classificationScheme, replacements, notReplaced)
        parentClassifier = ItemReferencerUtils.replaceItemByIdentity(parentClassifier, replacements, notReplaced)
        childClassifiers = ItemReferencerUtils.replaceItemsByIdentity(childClassifiers, replacements, notReplaced)
    }

    static Classifier build(
        Map args,
        @DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        new Classifier(args).tap(closure)
    }

    static Classifier build(@DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        build [:], closure
    }

    Classifier classifier(Classifier classifier) {
        this.classifiers.add(classifier)
        classifier.parentClassifier = this
        this.classificationScheme.classifiers.add(classifier)
        classifier
    }

    Classifier classifier(Map args, @DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        Classifier classifier = build(args + [classificationScheme: this.classificationScheme], closure)
        this.classifiers.add(classifier)
        classifier.parentClassifier = this
        this.classificationScheme.classifiers.add(classifier)
        classifier
    }

    Classifier classifier(@DelegatesTo(value = Classifier, strategy = Closure.DELEGATE_FIRST) Closure closure = {}) {
        classifier [:], closure
    }
}
