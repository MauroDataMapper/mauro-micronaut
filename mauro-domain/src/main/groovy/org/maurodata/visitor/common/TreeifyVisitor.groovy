package org.maurodata.visitor.common

import groovy.transform.CompileStatic
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.dataflow.DataClassComponent
import org.maurodata.domain.dataflow.DataElementComponent
import org.maurodata.domain.dataflow.DataFlow
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler
import org.maurodata.visitor.VisitorRegistry

@CompileStatic
class TreeifyVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    TreeifyVisitor() {
        addHandler(this)
    }

    // Direct dispatch avoids per-node closure calls; order matches the original handler registration order.
    @Override
    void enter(Item item) {
        if (item instanceof Folder) {
            ((Folder) item).parent = null
        }
        if (item instanceof Model) {
            ((Model) item).folder = null
        }
        if (item instanceof AdministeredItem) {
            AdministeredItem administeredItem = (AdministeredItem) item
            administeredItem.classifiers = administeredItem.classifiers.collect {classifier -> replaceWithStub(classifier)}
        }
        if (item instanceof DataModel) {
            DataModel dataModel = (DataModel) item
            dataModel.dataElements = []
            dataModel.allDataClasses = []
            dataModel.enumerationValues = []
        }
        if (item instanceof DataClass) {
            DataClass dataClass = (DataClass) item
            dataClass.extendedBy = []
            dataClass.extendsDataClasses = replaceWithStub(dataClass.extendsDataClasses) as List<DataClass>
            dataClass.referenceTypes = []
            dataClass.dataModel = null
        }
        if (item instanceof DataElement) {
            ((DataElement) item).dataModel = null
        }
        if (item instanceof DataType) {
            DataType dataType = (DataType) item
            dataType.referenceClass = replaceWithStub(dataType.referenceClass)
            dataType.modelResource = replaceWithStub(dataType.modelResource)
        }
        if (item instanceof EnumerationValue) {
            ((EnumerationValue) item).dataModel = null
        }
        if (item instanceof Term) {
            ((Term) item).terminology = null
        }
        if (item instanceof TermRelationshipType) {
            ((TermRelationshipType) item).terminology = null
        }
        if (item instanceof TermRelationship) {
            TermRelationship relationship = (TermRelationship) item
            relationship.sourceTerm = replaceWithStub(relationship.sourceTerm)
            relationship.targetTerm = replaceWithStub(relationship.targetTerm)
            relationship.relationshipType = replaceWithStub(relationship.relationshipType)
            relationship.terminology = null
        }
        if (item instanceof CodeSet) {
            CodeSet codeSet = (CodeSet) item
            codeSet.terms = codeSet.terms.collect {term -> replaceWithStub(term)} as Set<Term>
        }
        if (item instanceof Classifier) {
            ((Classifier) item).classificationScheme = null
        }
        if (item instanceof DataFlow) {
            DataFlow dataFlow = (DataFlow) item
            dataFlow.source = replaceWithStub(dataFlow.source)
            dataFlow.target = replaceWithStub(dataFlow.target)
        }
        if (item instanceof DataClassComponent) {
            DataClassComponent component = (DataClassComponent) item
            component.sourceDataClasses = component.sourceDataClasses.collect {replaceWithStub(it)}
            component.targetDataClasses = component.targetDataClasses.collect {replaceWithStub(it)}
            component.dataFlow = null
        }
        if (item instanceof DataElementComponent) {
            DataElementComponent component = (DataElementComponent) item
            component.sourceDataElements = component.sourceDataElements.collect {replaceWithStub(it)}
            component.targetDataElements = component.targetDataElements.collect {replaceWithStub(it)}
            component.dataClassComponent = null
        }
        if (item instanceof SemanticLink) {
            SemanticLink semanticLink = (SemanticLink) item
            semanticLink.target = replaceWithStub(semanticLink.target)
        }
        if (item instanceof VersionLink) {
            VersionLink versionLink = (VersionLink) item
            versionLink.target = replaceWithStub(versionLink.target)
        }
    }

    @Override
    void leave(Item item) {
    }

    private static <T extends AdministeredItem> T replaceWithStub(T administeredItem) {
        if (!administeredItem) {
            return null
        }
        Class<T> itemClass = (Class<T>) administeredItem.class
        T stub = itemClass.getDeclaredConstructor().newInstance()
        stub.id = administeredItem.id
        stub.label = administeredItem.label

        if(administeredItem instanceof EnumerationValue && stub instanceof EnumerationValue) {
            stub.key = administeredItem.key
        }
        if(administeredItem instanceof Term && stub instanceof Term) {
            stub.code = administeredItem.code
        }

        return stub
    }

    private static <T extends AdministeredItem> Collection<T> replaceWithStub(Collection<T> administeredItems) {
        if (!administeredItems) {
            return null
        }
        Collection<T> replacements = []
        replacements.addAll(administeredItems.collect {replaceWithStub(it)})
        return replacements
    }

}
