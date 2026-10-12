package org.maurodata.shredder

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
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

import org.maurodata.visitor.GenericDomainTraversalVisitor
import org.maurodata.visitor.ItemHandler
import org.maurodata.domain.model.Item

/**
 * Visitor that traverses the domain hierarchy and shreds items into organized collections,
 * tracking depth for hierarchical structures (folders, data classes, annotations).
 *
 * Uses a stack-based approach to track depth as we traverse nested hierarchies.
 */
@CompileStatic
class ShredVisitor extends GenericDomainTraversalVisitor implements ItemHandler {

    ShreddedContent shreddedContent = new ShreddedContent()

    // Stack-based depth tracking for nested hierarchies
    private int folderDepth = 0
    private int dataClassDepth = 0
    private int annotationDepth = 0

    ShredVisitor(ShreddedContent shreddedContent) {
        super()
        this.shreddedContent = shreddedContent
        addHandler(this)
    }

    ShredVisitor() {
        super()
        addHandler(this)
    }

    // Direct dispatch avoids per-node closure calls; order matches the original handler registration order.
    @Override
    void enter(Item item) {
        if (item instanceof AdministeredItem) {
            AdministeredItem administeredItem = (AdministeredItem) item
            if (administeredItem.classifiers) {
                shreddedContent.classifierJoins.put(administeredItem, administeredItem.classifiers.id as Set<UUID>)
            }
        }
        if (item instanceof Folder) {
            shreddedContent.addFolderAtDepth((Folder) item, folderDepth++)
        }
        if (item instanceof ClassificationScheme) {
            shreddedContent.classificationSchemes.add((ClassificationScheme) item)
        }
        if (item instanceof Classifier) {
            shreddedContent.classifiers.add((Classifier) item)
        }
        if (item instanceof DataModel) {
            shreddedContent.dataModels.add((DataModel) item)
        }
        if (item instanceof DataClass) {
            shreddedContent.addDataClassAtDepth((DataClass) item, dataClassDepth++)
        }
        if (item instanceof DataType) {
            shreddedContent.dataTypes.add((DataType) item)
        }
        if (item instanceof DataElement) {
            shreddedContent.dataElements.add((DataElement) item)
        }
        if (item instanceof EnumerationValue) {
            shreddedContent.enumerationValues.add((EnumerationValue) item)
        }
        if (item instanceof Terminology) {
            shreddedContent.terminologies.add((Terminology) item)
        }
        if (item instanceof Term) {
            shreddedContent.terms.add((Term) item)
        }
        if (item instanceof TermRelationshipType) {
            shreddedContent.termRelationshipTypes.add((TermRelationshipType) item)
        }
        if (item instanceof TermRelationship) {
            shreddedContent.termRelationships.add((TermRelationship) item)
        }
        if (item instanceof CodeSet) {
            shreddedContent.codeSets.add((CodeSet) item)
        }
        if (item instanceof DataFlow) {
            shreddedContent.dataFlows.add((DataFlow) item)
        }
        if (item instanceof DataClassComponent) {
            shreddedContent.dataClassComponents.add((DataClassComponent) item)
        }
        if (item instanceof DataElementComponent) {
            shreddedContent.dataElementComponents.add((DataElementComponent) item)
        }
        if (item instanceof Metadata) {
            shreddedContent.metadata.add((Metadata) item)
        }
        if (item instanceof VersionLink) {
            shreddedContent.versionLinks.add((VersionLink) item)
        }
        if (item instanceof SummaryMetadata) {
            shreddedContent.summaryMetadata.add((SummaryMetadata) item)
        }
        if (item instanceof SummaryMetadataReport) {
            shreddedContent.summaryMetadataReports.add((SummaryMetadataReport) item)
        }
        if (item instanceof SemanticLink) {
            shreddedContent.semanticLinks.add((SemanticLink) item)
        }
        if (item instanceof Rule) {
            shreddedContent.rules.add((Rule) item)
        }
        if (item instanceof RuleRepresentation) {
            shreddedContent.ruleRepresentations.add((RuleRepresentation) item)
        }
        if (item instanceof ReferenceFile) {
            shreddedContent.referenceFiles.add((ReferenceFile) item)
        }
        if (item instanceof Edit) {
            shreddedContent.edits.add((Edit) item)
        }
        if (item instanceof Annotation) {
            shreddedContent.addAnnotationAtDepth((Annotation) item, annotationDepth++)
        }
    }

    @Override
    void leave(Item item) {
        if (item instanceof Folder && folderDepth != 0) {
            folderDepth--
        }
        if (item instanceof DataClass && dataClassDepth != 0) {
            dataClassDepth--
        }
        if (item instanceof Annotation && annotationDepth != 0) {
            annotationDepth--
        }
    }
}


