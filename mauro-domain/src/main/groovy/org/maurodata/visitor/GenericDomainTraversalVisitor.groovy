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

    protected final void visitNode(Item item, Closure<?> traverseChildren) {
        registry.applyEnter(item)
        traverseChildren.call()
        if (item instanceof AdministeredItem) {
            traverseFacets((AdministeredItem) item)
        }
        registry.applyLeave(item)
    }

    protected final void applyEnter(Item item) {
        registry.applyEnter(item)
    }

    protected final void applyLeave(Item item) {
        registry.applyLeave(item)
    }

    @Override
    final Void visitFolder(Folder folder) {
        visitNode(folder) {
            traverseAll(folder.childFolders)
            traverseAll(folder.dataModels)
            traverseAll(folder.terminologies)
            traverseAll(folder.codeSets)
            traverseAll(folder.classificationSchemes)
        }
        return null
    }

    @Override
    final Void visitAnnotation(Annotation annotation) {
        visitNode(annotation) {
            traverseAll(annotation.childAnnotations)
        }
        return null
    }

    @Override
    final Void visitEdit(Edit edit) {
        visitNode(edit) {}
        return null
    }

    @Override
    final Void visitMetadata(Metadata metadata) {
        visitNode(metadata) {}
        return null
    }

    @Override
    final Void visitReferenceFile(ReferenceFile referenceFile) {
        visitNode(referenceFile) {}
        return null
    }

    @Override
    final Void visitRule(Rule rule) {
        visitNode(rule) {
            traverseAll(rule.ruleRepresentations)
        }
        return null
    }

    @Override
    final Void visitRuleRepresentation(RuleRepresentation ruleRepresentation) {
        visitNode(ruleRepresentation) { }
        return null
    }

    @Override
    final Void visitSemanticLink(SemanticLink semanticLink) {
        visitNode(semanticLink) { }
        return null
    }

    @Override
    final Void visitSummaryMetadata(SummaryMetadata summaryMetadata) {
        visitNode(summaryMetadata) {
            traverseAll(summaryMetadata.summaryMetadataReports)
        }
        return null
    }

    @Override
    final Void visitSummaryMetadataReport(SummaryMetadataReport summaryMetadataReport) {
        visitNode(summaryMetadataReport) { }
        return null
    }

    @Override
    final Void visitVersionLink(VersionLink versionLink) {
        visitNode(versionLink) { }
        return null
    }

    @Override
    final Void visitDataModel(DataModel dataModel) {
        visitNode(dataModel) {
            traverseAll(dataModel.dataClasses)
            traverseAll(dataModel.dataTypes)
        }
        return null
    }

    @Override
    final Void visitDataFlow(DataFlow dataFlow) {
        visitNode(dataFlow) {
            traverseAll(dataFlow.dataClassComponents)
        }
        return null
    }

    @Override
    final Void visitDataClassComponent(DataClassComponent dataClassComponent) {
        visitNode(dataClassComponent) {
            traverseAll(dataClassComponent.dataElementComponents)
        }
        return null
    }

    @Override
    final Void visitDataElementComponent(DataElementComponent dataElementComponent) {
        visitNode(dataElementComponent) { }
        return null
    }

    @Override
    final Void visitDataClass(DataClass dataClass) {
        visitNode(dataClass) {
            traverseAll(dataClass.dataClasses)
            traverseAll(dataClass.dataElements)
        }
        return null
    }

    @Override
    final Void visitDataElement(DataElement dataElement) {
        visitNode(dataElement) { }
        return null
    }

    @Override
    final Void visitDataType(DataType dataType) {
        visitNode(dataType) {
            traverseAll(dataType.enumerationValues)
        }
        return null
    }

    @Override
    final Void visitEnumerationValue(EnumerationValue enumerationValue) {
        visitNode (enumerationValue) { }
        return null
    }

    @Override
    final Void visitTerminology(Terminology terminology) {
        visitNode(terminology) {
            traverseAll(terminology.terms)
            traverseAll(terminology.termRelationshipTypes)
            traverseAll(terminology.termRelationships)
        }
        return null
    }

    @Override
    final Void visitTerm(Term term) {
        visitNode (term) { }
        return null
    }

    @Override
    final Void visitTermRelationshipType(TermRelationshipType termRelationshipType) {
        visitNode(termRelationshipType) { }
        return null
    }

    @Override
    final Void visitTermRelationship(TermRelationship termRelationship) {
        visitNode(termRelationship) { }
        return null
    }

    @Override
    final Void visitCodeSet(CodeSet codeSet) {
        visitNode(codeSet) { }
        return null
    }

    @Override
    final Void visitClassificationScheme(ClassificationScheme classificationScheme) {
        visitNode(classificationScheme) {
            traverseAll(classificationScheme.csClassifiers)
        }
        return null
    }

    @Override
    final Void visitClassifier(Classifier classifier) {
        visitNode(classifier) {
            traverseAll(classifier.childClassifiers)
        }
        return null
    }
}


