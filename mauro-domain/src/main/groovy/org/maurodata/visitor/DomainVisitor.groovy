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
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology

@CompileStatic
interface DomainVisitor<T> {

    T visitFolder(Folder folder)

    T visitAnnotation(Annotation annotation)

    T visitEdit(Edit edit)

    T visitMetadata(Metadata metadata)

    T visitReferenceFile(ReferenceFile referenceFile)

    T visitRule(Rule rule)

    T visitRuleRepresentation(RuleRepresentation ruleRepresentation)

    T visitSemanticLink(SemanticLink semanticLink)

    T visitSummaryMetadata(SummaryMetadata summaryMetadata)

    T visitSummaryMetadataReport(SummaryMetadataReport summaryMetadataReport)

    T visitVersionLink(VersionLink versionLink)

    T visitDataModel(DataModel dataModel)

    T visitDataFlow(DataFlow dataFlow)

    T visitDataClassComponent(DataClassComponent dataClassComponent)

    T visitDataElementComponent(DataElementComponent dataElementComponent)

    T visitDataClass(DataClass dataClass)

    T visitDataElement(DataElement dataElement)

    T visitDataType(DataType dataType)

    T visitEnumerationValue(EnumerationValue enumerationValue)

    T visitTerminology(Terminology terminology)

    T visitTerm(Term term)

    T visitTermRelationshipType(TermRelationshipType termRelationshipType)

    T visitTermRelationship(TermRelationship termRelationship)

    T visitCodeSet(CodeSet codeSet)

    T visitClassificationScheme(ClassificationScheme classificationScheme)

    T visitClassifier(Classifier classifier)
}