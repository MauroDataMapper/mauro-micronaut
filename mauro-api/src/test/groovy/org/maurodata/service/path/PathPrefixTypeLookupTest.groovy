package org.maurodata.service.path


import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
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
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.TermRelationship
import org.maurodata.domain.terminology.TermRelationshipType
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import spock.lang.Specification
import spock.lang.Unroll

@MicronautTest
class PathPrefixTypeLookupTest extends Specification {

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemCacheableRepositories

    @Unroll
    void 'PathPrefixTypeLookup -should get #expectedDomainType for #pathPrefix'() {
        PathPrefixTypeLookup pathPrefixTypeLookup = new PathPrefixTypeLookup(administeredItemCacheableRepositories)

        when:
        String domainType = pathPrefixTypeLookup.getDomainType(pathPrefix)

        then:
        domainType == expectedDomainType

        where:
        pathPrefix | expectedDomainType
        'fo'       | Folder.simpleName
        'vf'       | Folder.simpleName
        'te'       | Terminology.simpleName
        'tm'       | Term.simpleName
        'trt'      | TermRelationshipType.simpleName
        'tr'       | TermRelationship.simpleName
        'cs'       | CodeSet.simpleName
        'df'       | DataFlow.simpleName
        'dcc'      | DataClassComponent.simpleName
        'dec'      | DataElementComponent.simpleName
        'dm'       | DataModel.simpleName
        'dc'       | DataClass.simpleName
        'dt'       | DataType.simpleName
        'de'       | DataElement.simpleName
        'ev'       | EnumerationValue.simpleName
        'csc'      | ClassificationScheme.simpleName
        'cl'       | Classifier.simpleName
        'FO'       | Folder.simpleName
        'vF'       | Folder.simpleName
        'DM'       | DataModel.simpleName
        'dC'       | DataClass.simpleName
        'DCC'      | DataClassComponent.simpleName
        'CSC'      | ClassificationScheme.simpleName
    }
}

