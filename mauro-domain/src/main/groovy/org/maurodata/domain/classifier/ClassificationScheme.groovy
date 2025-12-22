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
class ClassificationScheme extends Model implements ItemReferencer {

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
        if (folder != null) {
            return "${folder.getDiffIdentifier()}|${getPathNodeString()}"
        }
        return "${getPathNodeString()}"
    }

    @Override
    void copyInto(Item into) {
        super.copyInto(into)
        ClassificationScheme intoClassificationScheme = (ClassificationScheme) into
        intoClassificationScheme.csClassifiers = ItemUtils.copyItems(this.csClassifiers, intoClassificationScheme.csClassifiers)
        intoClassificationScheme.breadcrumbTreeId = ItemUtils.copyItem(this.breadcrumbTreeId, intoClassificationScheme.breadcrumbTreeId)
    }

    @Override
    Item shallowCopy() {
        ClassificationScheme classificationSchemeShallowCopy = new ClassificationScheme()
        this.copyInto(classificationSchemeShallowCopy)
        return classificationSchemeShallowCopy
    }

    @Transient
    @JsonIgnore
    @Override
    List<ItemReference> retrieveItemReferences() {
        List<ItemReference> pathsBeingReferenced = [] + super.retrieveItemReferences()

        ItemReferencerUtils.addItem(parent, pathsBeingReferenced)
        ItemReferencerUtils.addItems(csClassifiers, pathsBeingReferenced)

        return pathsBeingReferenced
    }

    @Transient
    @JsonIgnore
    @Override
    void replaceItemReferencesByIdentity(IdentityHashMap<Item, Item> replacements, Map<UUID, Item> allItemsById, List<Item> notReplaced) {
        super.replaceItemReferencesByIdentity(replacements, allItemsById, notReplaced)
        parent = ItemReferencerUtils.replaceItemByIdentity(parent, replacements, notReplaced)
        csClassifiers = ItemReferencerUtils.replaceItemsByIdentity(csClassifiers, replacements, notReplaced)
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