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
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

import org.maurodata.visitor.GenericDomainTraversalVisitor

/**
 * Visitor that traverses the domain hierarchy and shreds items into organized collections,
 * tracking depth for hierarchical structures (folders, data classes, annotations).
 *
 * Uses a stack-based approach to track depth as we traverse nested hierarchies.
 */
@CompileStatic
class ShredVisitor extends GenericDomainTraversalVisitor {

    ShreddedContent shreddedContent = new ShreddedContent()

    // Stack-based depth tracking for nested hierarchies
    private int folderDepth = 0
    private int dataClassDepth = 0
    private int annotationDepth = 0

    ShredVisitor(ShreddedContent shreddedContent) {
        super()
        this.shreddedContent = shreddedContent
        registerHandlers()
    }

    ShredVisitor() {
        super()
        registerHandlers()
    }

    void registerHandlers() {
        // Folders - track depth with stack
        onEnter(Folder) { Folder folder ->
            shreddedContent.addFolderAtDepth(folder, folderDepth++)
        }

        onLeave(Folder) { Folder folder ->
            if (folderDepth != 0) {
                folderDepth --
            }
        }

        // Classification Schemes
        onEnter(ClassificationScheme) { ClassificationScheme scheme ->
            shreddedContent.classificationSchemes.add(scheme)
        }

        // Classifiers
        onEnter(Classifier) { Classifier classifier ->
            shreddedContent.classifiers.add(classifier)
        }

        // Data Models
        onEnter(DataModel) { DataModel dataModel ->
            shreddedContent.dataModels.add(dataModel)
        }

        // Data Classes - track depth with stack
        onEnter(DataClass) { DataClass dataClass ->
            shreddedContent.addDataClassAtDepth(dataClass, dataClassDepth++)
        }

        onLeave(DataClass) { DataClass dataClass ->
            if (dataClassDepth != 0) {
                dataClassDepth--
            }
        }

        // Data Types
        onEnter(DataType) { DataType dataType ->
            shreddedContent.dataTypes.add(dataType)
        }

        // Data Elements
        onEnter(DataElement) { DataElement dataElement ->
            shreddedContent.dataElements.add(dataElement)
        }

        // Enumeration Values
        onEnter(EnumerationValue) { EnumerationValue enumerationValue ->
            shreddedContent.enumerationValues.add(enumerationValue)
        }

        // Terminologies
        onEnter(Terminology) { Terminology terminology ->
            shreddedContent.terminologies.add(terminology)
        }

        // Terms
        onEnter(Term) { Term term ->
            shreddedContent.terms.add(term)
        }

        // Term Relationship Types
        onEnter(TermRelationshipType) { TermRelationshipType termRelationshipType ->
            shreddedContent.termRelationshipTypes.add(termRelationshipType)
        }

        // Term Relationships
        onEnter(TermRelationship) { TermRelationship termRelationship ->
            shreddedContent.termRelationships.add(termRelationship)
        }

        // Code Sets
        onEnter(CodeSet) { CodeSet codeSet ->
            shreddedContent.codeSets.add(codeSet)
        }

        // Data Flows
        onEnter(DataFlow) { DataFlow dataFlow ->
            shreddedContent.dataFlows.add(dataFlow)
        }

        // Data Class Components
        onEnter(DataClassComponent) { DataClassComponent dataClassComponent ->
            shreddedContent.dataClassComponents.add(dataClassComponent)
        }

        // Data Element Components
        onEnter(DataElementComponent) { DataElementComponent dataElementComponent ->
            shreddedContent.dataElementComponents.add(dataElementComponent)
        }

        onEnter(Metadata) {Metadata metadata ->
            shreddedContent.metadata.add(metadata)
        }

        onEnter(VersionLink) {VersionLink versionLink ->
            shreddedContent.versionLinks.add(versionLink)
        }

        onEnter(SummaryMetadata) {SummaryMetadata summaryMetadata ->
            shreddedContent.summaryMetadata.add(summaryMetadata)
        }
        onEnter(SummaryMetadataReport) {SummaryMetadataReport summaryMetadataReport ->
            shreddedContent.summaryMetadataReports.add(summaryMetadataReport)
        }

        onEnter(SemanticLink) {SemanticLink semanticLink ->
            shreddedContent.semanticLinks.add(semanticLink)
        }

        onEnter(Rule) {Rule rule ->
            shreddedContent.rules.add(rule)
        }
        onEnter(RuleRepresentation) {RuleRepresentation ruleRepresentation ->
            shreddedContent.ruleRepresentations.add(ruleRepresentation)
        }
        onEnter(ReferenceFile) {ReferenceFile referenceFile ->
            shreddedContent.referenceFiles.add(referenceFile)
        }
        onEnter(Edit) {Edit edit ->
            shreddedContent.edits.add(edit)
        }

        // Data Classes - track depth with stack
        onEnter(Annotation) { Annotation annotation ->
            shreddedContent.addAnnotationAtDepth(annotation, annotationDepth++)
        }

        onLeave(Annotation) { Annotation annotation ->
            if (annotationDepth != 0) {
                annotationDepth--
            }
        }

    }
}


