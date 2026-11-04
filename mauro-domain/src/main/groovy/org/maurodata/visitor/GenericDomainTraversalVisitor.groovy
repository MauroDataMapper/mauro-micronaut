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
import org.maurodata.domain.facet.RuleRepresentation
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
import org.maurodata.domain.overlay.Overlay

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

    <T extends Item> GenericDomainTraversalVisitor onEnter(Class<T> type, Closure<?> handler) {
        registry.onEnter(type, handler)
        return this
    }

    <T extends Item> GenericDomainTraversalVisitor onLeave(Class<T> type, Closure<?> handler) {
        registry.onLeave(type, handler)
        return this
    }

    GenericDomainTraversalVisitor plus(VisitorRegistry other) {
        registry.addAll(other)
        return this
    }

    GenericDomainTraversalVisitor plus(GenericDomainTraversalVisitor other) {
        registry.addAll(other.registry)
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
        for (Item item : items) {
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

    protected final void visitNode(Item item, Closure<?> traverseChildren) {
        applyEnter(item)
        traverseChildren.call()
        leaveNode(item)
    }

    GenericDomainTraversalVisitor addHandler(ItemHandler handler) {
        registry.addHandler(handler)
        return this
    }

    protected final void applyEnter(Item item) {
        registry.applyEnter(item)
    }

    protected final void applyLeave(Item item) {
        registry.applyLeave(item)
    }

    protected final void leaveNode(Item item) {
        if (item instanceof AdministeredItem) {
            traverseFacets((AdministeredItem) item)
        }
        applyLeave(item)
    }

    @Override
    final Void visitFolder(Folder folder) {
        applyEnter(folder)
        traverseAll(folder.childFolders)
        traverseAll(folder.dataModels)
        traverseAll(folder.terminologies)
        traverseAll(folder.codeSets)
        traverseAll(folder.classificationSchemes)
        traverseAll(folder.overlays)
        leaveNode(folder)
        return null
    }

    @Override
    final Void visitAnnotation(Annotation annotation) {
        applyEnter(annotation)
        traverseAll(annotation.childAnnotations)
        leaveNode(annotation)
        return null
    }

    @Override
    final Void visitEdit(Edit edit) {
        applyEnter(edit)
        leaveNode(edit)
        return null
    }

    @Override
    final Void visitMetadata(Metadata metadata) {
        applyEnter(metadata)
        leaveNode(metadata)
        return null
    }

    @Override
    final Void visitReferenceFile(ReferenceFile referenceFile) {
        applyEnter(referenceFile)
        leaveNode(referenceFile)
        return null
    }

    @Override
    final Void visitRule(Rule rule) {
        applyEnter(rule)
        traverseAll(rule.ruleRepresentations)
        leaveNode(rule)
        return null
    }

    @Override
    final Void visitRuleRepresentation(RuleRepresentation ruleRepresentation) {
        applyEnter(ruleRepresentation)
        leaveNode(ruleRepresentation)
        return null
    }

    @Override
    final Void visitSemanticLink(SemanticLink semanticLink) {
        applyEnter(semanticLink)
        leaveNode(semanticLink)
        return null
    }

    @Override
    final Void visitSummaryMetadata(SummaryMetadata summaryMetadata) {
        applyEnter(summaryMetadata)
        traverseAll(summaryMetadata.summaryMetadataReports)
        leaveNode(summaryMetadata)
        return null
    }

    @Override
    final Void visitSummaryMetadataReport(SummaryMetadataReport summaryMetadataReport) {
        applyEnter(summaryMetadataReport)
        leaveNode(summaryMetadataReport)
        return null
    }

    @Override
    final Void visitVersionLink(VersionLink versionLink) {
        applyEnter(versionLink)
        leaveNode(versionLink)
        return null
    }

    @Override
    final Void visitDataModel(DataModel dataModel) {
        applyEnter(dataModel)
        traverseAll(dataModel.dataClasses)
        traverseAll(dataModel.dataTypes)
        leaveNode(dataModel)
        return null
    }

    @Override
    final Void visitDataFlow(DataFlow dataFlow) {
        applyEnter(dataFlow)
        traverseAll(dataFlow.dataClassComponents)
        leaveNode(dataFlow)
        return null
    }

    @Override
    final Void visitDataClassComponent(DataClassComponent dataClassComponent) {
        applyEnter(dataClassComponent)
        traverseAll(dataClassComponent.dataElementComponents)
        leaveNode(dataClassComponent)
        return null
    }

    @Override
    final Void visitDataElementComponent(DataElementComponent dataElementComponent) {
        applyEnter(dataElementComponent)
        leaveNode(dataElementComponent)
        return null
    }

    @Override
    final Void visitDataClass(DataClass dataClass) {
        applyEnter(dataClass)
        traverseAll(dataClass.dataClasses)
        traverseAll(dataClass.dataElements)
        leaveNode(dataClass)
        return null
    }

    @Override
    final Void visitDataElement(DataElement dataElement) {
        applyEnter(dataElement)
        leaveNode(dataElement)
        return null
    }

    @Override
    final Void visitDataType(DataType dataType) {
        applyEnter(dataType)
        traverseAll(dataType.enumerationValues)
        leaveNode(dataType)
        return null
    }

    @Override
    final Void visitEnumerationValue(EnumerationValue enumerationValue) {
        applyEnter(enumerationValue)
        leaveNode(enumerationValue)
        return null
    }

    @Override
    final Void visitTerminology(Terminology terminology) {
        applyEnter(terminology)
        traverseAll(terminology.terms)
        traverseAll(terminology.termRelationshipTypes)
        traverseAll(terminology.termRelationships)
        leaveNode(terminology)
        return null
    }

    @Override
    final Void visitTerm(Term term) {
        applyEnter(term)
        leaveNode(term)
        return null
    }

    @Override
    final Void visitTermRelationshipType(TermRelationshipType termRelationshipType) {
        applyEnter(termRelationshipType)
        leaveNode(termRelationshipType)
        return null
    }

    @Override
    final Void visitTermRelationship(TermRelationship termRelationship) {
        applyEnter(termRelationship)
        leaveNode(termRelationship)
        return null
    }

    @Override
    final Void visitCodeSet(CodeSet codeSet) {
        applyEnter(codeSet)
        leaveNode(codeSet)
        return null
    }

    @Override
    final Void visitClassificationScheme(ClassificationScheme classificationScheme) {
        applyEnter(classificationScheme)
        traverseAll(classificationScheme.csClassifiers)
        leaveNode(classificationScheme)
        return null
    }

    @Override
    final Void visitClassifier(Classifier classifier) {
        applyEnter(classifier)
        traverseAll(classifier.childClassifiers)
        leaveNode(classifier)
        return null
    }

    @Override
    final Void visitOverlay(Overlay overlay) {
        applyEnter(overlay)
        leaveNode(overlay)
        return null
    }
}
