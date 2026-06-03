package org.maurodata.visitor.common

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
import org.maurodata.visitor.VisitorRegistry

class TreeifyVisitor extends GenericDomainTraversalVisitor {

    TreeifyVisitor() {
        onEnter(Folder) {Folder folder ->
            folder.parent = null
        }
        onEnter(Model) {Model model ->
            model.folder = null
        }
        onEnter(AdministeredItem) {AdministeredItem administeredItem ->
            administeredItem.classifiers = administeredItem.classifiers.collect {classifier -> replaceWithStub(classifier)}
        }
        onEnter(DataModel) {DataModel dataModel ->
            dataModel.dataElements = []
            dataModel.allDataClasses = []
            dataModel.enumerationValues = []
        }
        onEnter(DataClass) {DataClass dataClass ->
            dataClass.extendedBy = []
            dataClass.referenceTypes = []
            dataClass.dataModel = null
        }
        onEnter(DataElement) {DataElement dataElement ->
            dataElement.dataModel = null
        }
        onEnter(DataType) {DataType dataType ->
            dataType.referenceClass = replaceWithStub(dataType.referenceClass)
            dataType.modelResource = replaceWithStub(dataType.modelResource)
        }
        onEnter(EnumerationValue) {EnumerationValue enumerationValue ->
            enumerationValue.dataModel = null
        }
        onEnter(Term) {Term term ->
            term.terminology = null
        }
        onEnter(TermRelationshipType) {TermRelationshipType relationshipType ->
            relationshipType.terminology = null
        }
        onEnter(TermRelationship) {TermRelationship relationship ->
            relationship.sourceTerm = replaceWithStub(relationship.sourceTerm)
            relationship.targetTerm = replaceWithStub(relationship.targetTerm)
            relationship.relationshipType = replaceWithStub(relationship.relationshipType)
            relationship.terminology = null
        }
        onEnter(CodeSet) {CodeSet codeSet ->
            codeSet.terms = codeSet.terms.collect {term -> replaceWithStub(term)} as Set<Term>
        }
        onEnter(Classifier) {Classifier classifier ->
            classifier.classificationScheme = null
        }
        onEnter(DataFlow) {DataFlow dataFlow ->
            dataFlow.source = replaceWithStub(dataFlow.source)
            dataFlow.target = replaceWithStub(dataFlow.target)
        }
        onEnter(DataClassComponent) {DataClassComponent component ->
            component.sourceDataClasses = component.sourceDataClasses.collect {replaceWithStub(it)}
            component.targetDataClasses = component.targetDataClasses.collect {replaceWithStub(it)}
            component.dataFlow = null
        }
        onEnter(DataElementComponent) {DataElementComponent component ->
            component.sourceDataElements = component.sourceDataElements.collect {replaceWithStub(it)}
            component.targetDataElements = component.targetDataElements.collect {replaceWithStub(it)}
            component.dataClassComponent = null
        }
        onEnter(SemanticLink) {SemanticLink semanticLink ->
            semanticLink.target = replaceWithStub(semanticLink.target)
        }
        onEnter(VersionLink) {VersionLink versionLink ->
            versionLink.target = replaceWithStub(versionLink.target)
        }
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

}
