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
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

/**
 * Legacy hook-based traversal visitor.
 *
 * Traversal order now lives in GenericDomainTraversalVisitor while this class
 * preserves the existing protected onVisitX extension points.
 */
@CompileStatic
abstract class AbstractDomainTraversalVisitor extends GenericDomainTraversalVisitor {

    AbstractDomainTraversalVisitor() {
        super()
        registerLegacyHooks()
    }

    AbstractDomainTraversalVisitor(VisitorRegistry registry) {
        super(registry)
        registerLegacyHooks()
    }

    private void registerLegacyHooks() {
        on(Folder) {Folder folder -> onVisitFolder(folder)}
        on(Annotation) {Annotation annotation -> onVisitAnnotation(annotation)}
        on(Edit) {Edit edit -> onVisitEdit(edit)}
        on(Metadata) {Metadata metadata -> onVisitMetadata(metadata)}
        on(ReferenceFile) {ReferenceFile referenceFile -> onVisitReferenceFile(referenceFile)}
        on(Rule) {Rule rule -> onVisitRule(rule)}
        on(SemanticLink) {SemanticLink semanticLink -> onVisitSemanticLink(semanticLink)}
        on(SummaryMetadata) {SummaryMetadata summaryMetadata -> onVisitSummaryMetadata(summaryMetadata)}
        on(SummaryMetadataReport) {SummaryMetadataReport report -> onVisitSummaryMetadataReport(report)}
        on(VersionLink) {VersionLink versionLink -> onVisitVersionLink(versionLink)}
        on(DataModel) {DataModel dataModel -> onVisitDataModel(dataModel)}
        on(DataFlow) {DataFlow dataFlow -> onVisitDataFlow(dataFlow)}
        on(DataClassComponent) {DataClassComponent dataClassComponent -> onVisitDataClassComponent(dataClassComponent)}
        on(DataElementComponent) {DataElementComponent dataElementComponent -> onVisitDataElementComponent(dataElementComponent)}
        on(DataClass) {DataClass dataClass -> onVisitDataClass(dataClass)}
        on(DataElement) {DataElement dataElement -> onVisitDataElement(dataElement)}
        on(DataType) {DataType dataType -> onVisitDataType(dataType)}
        on(EnumerationValue) {EnumerationValue enumerationValue -> onVisitEnumerationValue(enumerationValue)}
        on(Terminology) {Terminology terminology -> onVisitTerminology(terminology)}
        on(Term) {Term term -> onVisitTerm(term)}
        on(TermRelationshipType) {TermRelationshipType termRelationshipType -> onVisitTermRelationshipType(termRelationshipType)}
        on(TermRelationship) {TermRelationship termRelationship -> onVisitTermRelationship(termRelationship)}
        on(CodeSet) {CodeSet codeSet -> onVisitCodeSet(codeSet)}
        on(ClassificationScheme) {ClassificationScheme classificationScheme -> onVisitClassificationScheme(classificationScheme)}
        on(Classifier) {Classifier classifier -> onVisitClassifier(classifier)}
    }

    protected void onVisitFolder(Folder folder) {}

    protected void onVisitAnnotation(Annotation annotation) {}

    protected void onVisitEdit(Edit edit) {}

    protected void onVisitMetadata(Metadata metadata) {}

    protected void onVisitReferenceFile(ReferenceFile referenceFile) {}

    protected void onVisitRule(Rule rule) {}

    protected void onVisitSemanticLink(SemanticLink semanticLink) {}

    protected void onVisitSummaryMetadata(SummaryMetadata summaryMetadata) {}

    protected void onVisitSummaryMetadataReport(SummaryMetadataReport summaryMetadataReport) {}

    protected void onVisitVersionLink(VersionLink versionLink) {}

    protected void onVisitDataModel(DataModel dataModel) {}

    protected void onVisitDataFlow(DataFlow dataFlow) {}

    protected void onVisitDataClassComponent(DataClassComponent dataClassComponent) {}

    protected void onVisitDataElementComponent(DataElementComponent dataElementComponent) {}

    protected void onVisitDataClass(DataClass dataClass) {}

    protected void onVisitDataElement(DataElement dataElement) {}

    protected void onVisitDataType(DataType dataType) {}

    protected void onVisitEnumerationValue(EnumerationValue enumerationValue) {}

    protected void onVisitTerminology(Terminology terminology) {}

    protected void onVisitTerm(Term term) {}

    protected void onVisitTermRelationshipType(TermRelationshipType termRelationshipType) {}

    protected void onVisitTermRelationship(TermRelationship termRelationship) {}

    protected void onVisitCodeSet(CodeSet codeSet) {}

    protected void onVisitClassificationScheme(ClassificationScheme classificationScheme) {}

    protected void onVisitClassifier(Classifier classifier) {}
}

