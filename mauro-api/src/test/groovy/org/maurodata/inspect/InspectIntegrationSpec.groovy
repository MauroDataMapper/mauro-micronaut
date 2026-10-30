package org.maurodata.inspect

import io.micronaut.test.annotation.Sql
import jakarta.inject.Singleton
import org.maurodata.api.facet.SemanticLinkCreateDTO
import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.datamodel.EnumerationValue
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.classifier.Classifier
import org.maurodata.domain.facet.Metadata
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.testing.CommonDataSpec

@ContainerizedTest
@Singleton
@Sql(scripts = ["classpath:sql/tear-down-classifiers.sql", "classpath:sql/tear-down-datamodel.sql", "classpath:sql/tear-down.sql", "classpath:sql/tear-down-folder.sql"], phase = Sql.Phase.AFTER_EACH)
class InspectIntegrationSpec extends CommonDataSpec {

    void 'inspect data model returns recursive children and inspect-only data types'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec folder'))
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('InspectIntegrationSpec data model'))
        DataType primitiveType = dataTypeApi.create(dataModel.id, dataTypesPayload('InspectIntegrationSpec string', DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataType enumerationType = dataTypeApi.create(dataModel.id, enumerationValueDataTypePayload('InspectIntegrationSpec yes no'))
        EnumerationValue enumerationValue = enumerationValueApi.create(dataModel.id, enumerationType.id, new EnumerationValue(key: 'Y', value: 'Yes'))
        DataClass parentDataClass = dataClassApi.create(dataModel.id, dataClassPayload('InspectIntegrationSpec parent class'))
        DataClass childDataClass = dataClassApi.create(dataModel.id, parentDataClass.id, dataClassPayload('InspectIntegrationSpec child class'))
        DataElement dataElement = dataElementApi.create(dataModel.id, childDataClass.id, dataElementPayload('InspectIntegrationSpec answer', enumerationType))
        ClassificationScheme classificationScheme = classificationSchemeApi.create(folder.id, new ClassificationScheme(label: 'InspectIntegrationSpec scheme'))
        Classifier classifier = classifierApi.create(classificationScheme.id, new Classifier(label: 'InspectIntegrationSpec classifier'))
        classifierApi.createAdministeredItemClassifier(DataModel.simpleName, dataModel.id, classifier.id)
        metadataApi.create(DataModel.simpleName, dataModel.id, new Metadata(
            namespace: 'org.maurodata.inspect',
            key: 'example',
            value: 'value'))

        when:
        Map<String, Object> inspected = inspectApi.inspect(DataModel.simpleName, dataModel.id)

        then:
        inspected.id == dataModel.id.toString()
        inspected.domainType == DataModel.simpleName
        inspected.label == dataModel.label
        inspected.path == [
            [prefix: 'fo', identifier: 'InspectIntegrationSpec folder'],
            [prefix: 'dm', identifier: 'InspectIntegrationSpec data model', modelIdentifier: 'main']
        ]
        inspected.keySet().toList() == [
            'label',
            'description',
            'id',
            'domainType',
            'versioning',
            'ownership',
            'type',
            'metadata',
            'classifiers',
            'path',
            'dataClasses'
        ]
        inspected.versioning.finalised == false
        inspected.versioning.branchName == 'main'
        inspected.ownership == [author: 'test author']
        inspected.classifiers == [[id: classifier.id.toString(), label: classifier.label]]
        inspected.metadata == ['org.maurodata.inspect:example': 'value']
        !inspected.containsKey('dataTypes')

        and:
        Map<String, Object> inspectedDataType = (Map<String, Object>) inspected.dataClasses[0].dataClasses[0].dataElements[0].dataType
        inspectedDataType.id == enumerationType.id.toString()
        inspectedDataType.enumerationValues.size() == 1
        inspectedDataType.enumerationValues[0].id == enumerationValue.id.toString()
        !inspectedDataType.containsKey('dataClasses')

        and:
        inspected.dataClasses.size() == 1
        inspected.dataClasses[0].id == parentDataClass.id.toString()
        inspected.dataClasses[0].dataClasses.size() == 1
        inspected.dataClasses[0].dataClasses[0].id == childDataClass.id.toString()
        inspected.dataClasses[0].dataClasses[0].dataElements.size() == 1
        inspected.dataClasses[0].dataClasses[0].dataElements[0].id == dataElement.id.toString()
        inspected.dataClasses[0].dataClasses[0].dataElements[0].dataType.id == enumerationType.id.toString()
    }

    void 'inspect data type includes enumeration values but does not recurse as a child axis'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec data type folder'))
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('InspectIntegrationSpec data type model'))
        DataType enumerationType = dataTypeApi.create(dataModel.id, enumerationValueDataTypePayload('InspectIntegrationSpec enum'))
        EnumerationValue yes = enumerationValueApi.create(dataModel.id, enumerationType.id, new EnumerationValue(key: 'Y', value: 'Yes'))

        when:
        Map<String, Object> inspected = inspectApi.inspect(DataType.DataTypeKind.ENUMERATION_TYPE.stringValue, enumerationType.id)

        then:
        inspected.id == enumerationType.id.toString()
        inspected.domainType == DataType.DataTypeKind.ENUMERATION_TYPE.stringValue
        inspected.enumerationValues.size() == 1
        inspected.enumerationValues[0].id == yes.id.toString()
        !inspected.containsKey('type')
        !inspected.containsKey('ownership')
        !inspected.containsKey('dataClasses')
        !inspected.containsKey('dataElements')
    }

    void 'inspect code set includes referenced terms'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec code set folder'))
        Terminology terminology = terminologyApi.create(folder.id, new Terminology(label: 'InspectIntegrationSpec terminology'))
        Term term = termApi.create(terminology.id, new Term(code: 'Y', definition: 'Yes'))
        CodeSet codeSet = codeSetApi.create(folder.id, new CodeSet(label: 'InspectIntegrationSpec code set', terms: [[id: term.id] as Term] as Set<Term>))

        when:
        Map<String, Object> inspected = inspectApi.inspect(CodeSet.simpleName, codeSet.id)

        then:
        inspected.id == codeSet.id.toString()
        inspected.domainType == CodeSet.simpleName
        inspected.terms.size() == 1
        inspected.terms[0].id == term.id.toString()
        inspected.terms[0].code == term.code
        !inspected.terms[0].containsKey('sourceTermRelationships')
    }

    void 'inspect model versioning includes version links as model references'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec versioning folder'))
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('InspectIntegrationSpec versioned model'))
        DataModel finalised = dataModelApi.finalise(dataModel.id, finalisePayload())
        DataModel branch = dataModelApi.createNewBranchModelVersion(finalised.id, new CreateNewVersionData(branchName: 'inspectBranch'))

        when:
        Map<String, Object> inspectedFinalised = inspectApi.inspect(DataModel.simpleName, finalised.id)
        Map<String, Object> inspectedBranch = inspectApi.inspect(DataModel.simpleName, branch.id)

        then:
        inspectedFinalised.versioning.versionLinks.size() == 1
        inspectedFinalised.versioning.versionLinks[0].linkType == 'NEW_MODEL_VERSION_OF'
        inspectedFinalised.versioning.versionLinks[0].sourceModel.id == branch.id.toString()
        inspectedFinalised.versioning.versionLinks[0].sourceModel.label == branch.label
        inspectedFinalised.versioning.versionLinks[0].sourceModel.branchName == 'inspectBranch'
        inspectedFinalised.versioning.versionLinks[0].targetModel.id == finalised.id.toString()
        inspectedFinalised.versioning.versionLinks[0].targetModel.modelVersion == finalised.modelVersion.toString()
        !inspectedFinalised.versioning.versionLinks[0].sourceModel.containsKey('dataClasses')
        !inspectedFinalised.versioning.versionLinks[0].sourceModel.containsKey('metadata')

        and:
        inspectedBranch.versioning.versionLinks.size() == 1
        inspectedBranch.versioning.versionLinks[0].sourceModel.id == branch.id.toString()
        inspectedBranch.versioning.versionLinks[0].targetModel.id == finalised.id.toString()

        when:
        String overview = inspectApi.overview(DataModel.simpleName, finalised.id)

        then:
        overview.contains('Versioning\n----------')
        overview.contains("o  main ${finalised.modelVersion} [finalised]".toString())
        overview.contains('└─ NEW_MODEL_VERSION_OF ──> o  inspectBranch [draft]')

        when:
        String branchOverview = inspectApi.overview(DataModel.simpleName, branch.id)

        then:
        branchOverview.contains('└─ HAS_NEW_MODEL_VERSION ──> o  main 1.0.0 [finalised]')
    }

    void 'inspect data element includes semantic links as references'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec semantic links folder'))
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('InspectIntegrationSpec semantic links model'))
        DataType dataType = dataTypeApi.create(dataModel.id, dataTypesPayload('InspectIntegrationSpec semantic links string', DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('InspectIntegrationSpec semantic links class'))
        DataElement source = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('InspectIntegrationSpec source', dataType))
        DataElement target = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('InspectIntegrationSpec target', dataType))
        semanticLinksApi.create(DataElement.simpleName, source.id, new SemanticLinkCreateDTO(
            linkType: 'Refines',
            targetMultiFacetAwareItemDomainType: DataElement.simpleName,
            targetMultiFacetAwareItemId: target.id))

        when:
        Map<String, Object> inspectedSource = inspectApi.inspect(DataElement.simpleName, source.id)
        Map<String, Object> inspectedTarget = inspectApi.inspect(DataElement.simpleName, target.id)

        then:
        inspectedSource.links.size() == 1
        inspectedSource.links[0].domainType == 'SemanticLink'
        inspectedSource.links[0].linkType == 'Refines'
        inspectedSource.links[0].unconfirmed == false
        inspectedSource.links[0].sourceMultiFacetAwareItem == [
            id        : source.id.toString(),
            domainType: DataElement.simpleName,
            label     : source.label
        ]
        inspectedSource.links[0].targetMultiFacetAwareItem == [
            id        : target.id.toString(),
            domainType: DataElement.simpleName,
            label     : target.label
        ]
        !inspectedSource.links[0].targetMultiFacetAwareItem.containsKey('metadata')
        !inspectedSource.links[0].targetMultiFacetAwareItem.containsKey('dataType')
        !inspectedSource.containsKey('semanticLinks')

        and:
        inspectedTarget.links.size() == 1
        inspectedTarget.links[0].sourceMultiFacetAwareItem.id == source.id.toString()
        inspectedTarget.links[0].targetMultiFacetAwareItem.id == target.id.toString()
        !inspectedTarget.containsKey('semanticLinks')
    }

    void 'overview returns structural tree and relationship arrows'() {
        given:
        Folder folder = folderApi.create(new Folder(label: 'InspectIntegrationSpec overview folder'))
        DataModel dataModel = dataModelApi.create(folder.id, dataModelPayload('InspectIntegrationSpec overview model'))
        DataType dataType = dataTypeApi.create(dataModel.id, dataTypesPayload('InspectIntegrationSpec overview string', DataType.DataTypeKind.PRIMITIVE_TYPE))
        DataClass dataClass = dataClassApi.create(dataModel.id, dataClassPayload('InspectIntegrationSpec overview class'))
        DataElement first = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('InspectIntegrationSpec overview first', dataType))
        DataElement second = dataElementApi.create(dataModel.id, dataClass.id, dataElementPayload('InspectIntegrationSpec overview second', dataType))
        semanticLinksApi.create(DataElement.simpleName, first.id, new SemanticLinkCreateDTO(
            linkType: 'Refines',
            targetMultiFacetAwareItemDomainType: DataElement.simpleName,
            targetMultiFacetAwareItemId: second.id))

        when:
        String overview = inspectApi.overview(DataModel.simpleName, dataModel.id)

        then:
        overview.startsWith('Description\n-----------\ntest description\n\nStructure\n---------\nDataModel: InspectIntegrationSpec overview model')
        overview.contains('DataModel: InspectIntegrationSpec overview model')
        overview.contains('└─ DataClass: InspectIntegrationSpec overview class')
        overview.contains('├─ DataElement: InspectIntegrationSpec overview first')
        overview.contains('└─ DataElement: InspectIntegrationSpec overview second')
        overview.contains('Relationships\n-------------')
        overview.contains('DataModel: InspectIntegrationSpec overview model / DataClass: InspectIntegrationSpec overview class / DataElement: InspectIntegrationSpec overview first --refines--> DataElement: InspectIntegrationSpec overview second')
        overview.contains('DataModel: InspectIntegrationSpec overview model / DataClass: InspectIntegrationSpec overview class / DataElement: InspectIntegrationSpec overview first --uses-type--> PrimitiveType: InspectIntegrationSpec overview string')
    }
}
