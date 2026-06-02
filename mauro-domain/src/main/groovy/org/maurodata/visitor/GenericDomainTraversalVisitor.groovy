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
import org.maurodata.domain.facet.Annotation
import org.maurodata.domain.facet.Edit
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.facet.ReferenceFile
import org.maurodata.domain.facet.Rule
import org.maurodata.domain.facet.SemanticLink
import org.maurodata.domain.facet.SummaryMetadata
import org.maurodata.domain.facet.SummaryMetadataReport
import org.maurodata.domain.facet.VersionLink
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.AdministeredItem
import org.maurodata.domain.model.Item
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

/**
 * Generic hook-driven traversal visitor for the Mauro domain hierarchy.
 */
@CompileStatic
class GenericDomainTraversalVisitor implements DomainVisitor<Void> {

    protected final Set<UUID> visitedIds = new HashSet<>()
    protected final Set<Item> visitedIdentity = Collections.newSetFromMap(new IdentityHashMap<Item, Boolean>())
    protected final VisitorRegistry registry

    GenericDomainTraversalVisitor() {
        this(new VisitorRegistry())
    }

    GenericDomainTraversalVisitor(VisitorRegistry registry) {
        this.registry = registry ?: new VisitorRegistry()
    }

    <T extends Item> GenericDomainTraversalVisitor on(Class<T> type, Closure<?> handler) {
        registry.on(type, handler)
        return this
    }

    GenericDomainTraversalVisitor plus(VisitorRegistry other) {
        registry.addAll(other)
        return this
    }

    void resetTraversalState() {
        visitedIds.clear()
        visitedIdentity.clear()
    }

    protected boolean shouldVisit(Item item) {
        if (!item) {
            return false
        }

        if (item.id && !visitedIds.add(item.id)) {
            return false
        }

        return visitedIdentity.add(item)
    }

    protected final void traverse(Item item) {
        if (shouldVisit(item)) {
            item.accept(this)
        }
    }

    protected final <I extends Item> void traverseAll(Collection<I> items) {
        if (!items) {
            return
        }
        items.each {Item item ->
            traverse(item)
        }
    }

    protected final void traverseFacets(AdministeredItem item) {
        if (!item) {
            return
        }
        traverseAll(item.edits)
        traverseAll(item.metadata)
        traverseAll(item.summaryMetadata)
        traverseAll(item.rules)
        traverseAll(item.annotations)
        traverseAll(item.referenceFiles)
        traverseAll(item.semanticLinks)
        if (item instanceof Model) {
            traverseAll(((Model) item).versionLinks)
        }
    }

    protected final void applyHandlers(Item item) {
        registry.apply(item)
    }

    @Override
    final Void visitFolder(Folder folder) {
        applyHandlers(folder)
        traverseAll(folder.childFolders)
        traverseAll(folder.dataModels)
        traverseAll(folder.terminologies)
        traverseAll(folder.codeSets)
        traverseAll(folder.classificationSchemes)
        traverseFacets(folder)
        return null
    }

    @Override
    final Void visitAnnotation(Annotation annotation) {
        applyHandlers(annotation)
        traverseAll(annotation.childAnnotations)
        return null
    }

    @Override
    final Void visitEdit(Edit edit) {
        applyHandlers(edit)
        return null
    }

    @Override
    final Void visitMetadata(Metadata metadata) {
        applyHandlers(metadata)
        return null
    }

    @Override
    final Void visitReferenceFile(ReferenceFile referenceFile) {
        applyHandlers(referenceFile)
        return null
    }

    @Override
    final Void visitRule(Rule rule) {
        applyHandlers(rule)
        return null
    }

    @Override
    final Void visitSemanticLink(SemanticLink semanticLink) {
        applyHandlers(semanticLink)
        return null
    }

    @Override
    final Void visitSummaryMetadata(SummaryMetadata summaryMetadata) {
        applyHandlers(summaryMetadata)
        traverseAll(summaryMetadata.summaryMetadataReports)
        return null
    }

    @Override
    final Void visitSummaryMetadataReport(SummaryMetadataReport summaryMetadataReport) {
        applyHandlers(summaryMetadataReport)
        return null
    }

    @Override
    final Void visitVersionLink(VersionLink versionLink) {
        applyHandlers(versionLink)
        return null
    }

    @Override
    final Void visitDataModel(DataModel dataModel) {
        applyHandlers(dataModel)
        traverseAll(dataModel.dataClasses)
        traverseAll(dataModel.dataTypes)
        traverseFacets(dataModel)
        return null
    }

    @Override
    final Void visitDataFlow(DataFlow dataFlow) {
        applyHandlers(dataFlow)
        traverseAll(dataFlow.dataClassComponents)
        traverseFacets(dataFlow)
        return null
    }

    @Override
    final Void visitDataClassComponent(DataClassComponent dataClassComponent) {
        applyHandlers(dataClassComponent)
        traverseAll(dataClassComponent.dataElementComponents)
        traverseFacets(dataClassComponent)
        return null
    }

    @Override
    final Void visitDataElementComponent(DataElementComponent dataElementComponent) {
        applyHandlers(dataElementComponent)
        traverseFacets(dataElementComponent)
        return null
    }

    @Override
    final Void visitDataClass(DataClass dataClass) {
        applyHandlers(dataClass)
        traverseAll(dataClass.dataClasses)
        traverseAll(dataClass.dataElements)
        traverseFacets(dataClass)
        return null
    }

    @Override
    final Void visitDataElement(DataElement dataElement) {
        applyHandlers(dataElement)
        traverseFacets(dataElement)
        return null
    }

    @Override
    final Void visitDataType(DataType dataType) {
        applyHandlers(dataType)
        traverseAll(dataType.enumerationValues)
        traverseFacets(dataType)
        return null
    }

    @Override
    final Void visitEnumerationValue(EnumerationValue enumerationValue) {
        applyHandlers(enumerationValue)
        traverseFacets(enumerationValue)
        return null
    }

    @Override
    final Void visitTerminology(Terminology terminology) {
        applyHandlers(terminology)
        traverseAll(terminology.terms)
        traverseAll(terminology.termRelationshipTypes)
        traverseAll(terminology.termRelationships)
        traverseFacets(terminology)
        return null
    }

    @Override
    final Void visitTerm(Term term) {
        applyHandlers(term)
        traverseFacets(term)
        return null
    }

    @Override
    final Void visitTermRelationshipType(TermRelationshipType termRelationshipType) {
        applyHandlers(termRelationshipType)
        traverseFacets(termRelationshipType)
        return null
    }

    @Override
    final Void visitTermRelationship(TermRelationship termRelationship) {
        applyHandlers(termRelationship)
        traverseFacets(termRelationship)
        return null
    }

    @Override
    final Void visitCodeSet(CodeSet codeSet) {
        applyHandlers(codeSet)
        traverseFacets(codeSet)
        return null
    }

    @Override
    final Void visitClassificationScheme(ClassificationScheme classificationScheme) {
        applyHandlers(classificationScheme)
        traverseAll(classificationScheme.csClassifiers)
        traverseFacets(classificationScheme)
        return null
    }

    @Override
    final Void visitClassifier(Classifier classifier) {
        applyHandlers(classifier)
        traverseAll(classifier.childClassifiers)
        traverseFacets(classifier)
        return null
    }
}


