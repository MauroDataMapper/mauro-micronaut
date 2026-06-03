package org.maurodata.visitor

import groovy.transform.CompileStatic
import org.maurodata.domain.classifier.ClassificationScheme
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

/**
 * Reusable bundles of visitor handlers for common traversal tasks.
 */
@CompileStatic
final class CommonVisitorRegistries {

    private CommonVisitorRegistries() {
    }

    static VisitorRegistry treeifyVisitor() {
        VisitorRegistry registry = new VisitorRegistry()
        registry.onEnter(Folder) { Folder folder ->
            folder.parent = null
        }
        registry.onEnter(Model) { Model model ->
            model.folder = null
        }
        registry.onEnter(AdministeredItem) { AdministeredItem administeredItem ->
            administeredItem.classifiers = administeredItem.classifiers.collect {classifier -> replaceWithStub(classifier)}
        }
        registry.onEnter(DataModel) {DataModel dataModel ->
            dataModel.dataElements = []
            dataModel.allDataClasses = []
            dataModel.enumerationValues = []
        }
        registry.onEnter(DataClass) {DataClass dataClass ->
            dataClass.extendedBy = []
            dataClass.referenceTypes = []
            dataClass.dataModel = null
        }
        registry.onEnter(DataElement) {DataElement dataElement ->
            dataElement.dataModel = null
        }
        registry.onEnter(DataType) {DataType dataType ->
            dataType.referenceClass = replaceWithStub(dataType.referenceClass)
            dataType.modelResource = replaceWithStub(dataType.modelResource)
        }
        registry.onEnter(EnumerationValue) {EnumerationValue enumerationValue ->
            enumerationValue.dataModel = null
        }
        registry.onEnter(Term) {Term term ->
            term.terminology = null
        }
        registry.onEnter(TermRelationshipType) {TermRelationshipType relationshipType ->
            relationshipType.terminology = null
        }
        registry.onEnter(TermRelationship) {TermRelationship relationship ->
            relationship.sourceTerm = replaceWithStub(relationship.sourceTerm)
            relationship.targetTerm = replaceWithStub(relationship.targetTerm)
            relationship.relationshipType = replaceWithStub(relationship.relationshipType)
            relationship.terminology = null
        }
        registry.onEnter(CodeSet) {CodeSet codeSet ->
            codeSet.terms = codeSet.terms.collect {term -> replaceWithStub(term)} as Set<Term>
        }
        registry.onEnter(Classifier) {Classifier classifier ->
            classifier.classificationScheme = null
        }
        registry.onEnter(DataFlow) {DataFlow dataFlow ->
            dataFlow.source = replaceWithStub(dataFlow.source)
            dataFlow.target = replaceWithStub(dataFlow.target)
        }
        registry.onEnter(DataClassComponent) {DataClassComponent component ->
            component.sourceDataClasses = component.sourceDataClasses.collect {replaceWithStub(it)}
            component.targetDataClasses = component.targetDataClasses.collect {replaceWithStub(it)}
            component.dataFlow = null
        }
        registry.onEnter(DataElementComponent) {DataElementComponent component ->
            component.sourceDataElements = component.sourceDataElements.collect {replaceWithStub(it)}
            component.targetDataElements = component.targetDataElements.collect {replaceWithStub(it)}
            component.dataClassComponent = null
        }
        registry.onEnter(SemanticLink) {SemanticLink semanticLink ->
            semanticLink.target = replaceWithStub(semanticLink.target)
        }
        registry.onEnter(VersionLink) {VersionLink versionLink ->
            versionLink.target = replaceWithStub(versionLink.target)
        }
        return registry
    }

    static VisitorRegistry smallExport() {
        VisitorRegistry registry = new VisitorRegistry()
        registry.onEnter(Item) {Item item ->
            item.dateCreated = null
            item.lastUpdated = null
            item.version = null
            item.catalogueUser = null
        }
        .onEnter(AdministeredItem) { AdministeredItem administeredItem ->
            administeredItem.edits = []

        }
        .onEnter(Model) { Model model ->
            model.readableByAuthenticatedUsers = null
            model.readableByEveryone = null
            model.finalised = null
            model.modelType = null
            model.deleted = null
        }
        return registry
    }

    private static <T extends AdministeredItem> T replaceWithStub(T administeredItem) {
        if (!administeredItem) {
            return null
        }
        Class<T> itemClass = (Class<T>) administeredItem.class
        T stub = itemClass.getDeclaredConstructor().newInstance()
        stub.id = administeredItem.id
        stub.label = administeredItem.label
        return stub
    }
}

