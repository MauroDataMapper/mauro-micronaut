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
class PathPrefixLoaderTest extends Specification {

    @Inject
    List<AdministeredItemCacheableRepository> administeredItemCacheableRepositories

    @Unroll
    def 'PathPrefixLoader -should get #expectedDomainType for #pathPrefix'() {
        PathPrefixLoader pathPrefixLoader = new PathPrefixLoader(administeredItemCacheableRepositories)

        when:
        String domainType = pathPrefixLoader.getDomainType(pathPrefix)

        then:
        domainType == expectedDomainType

        where:
        pathPrefix | expectedDomainType
        'fo'       | Folder.class.simpleName
        'vf'       | Folder.class.simpleName
        'te'       | Terminology.class.simpleName
        'tm'       | Term.class.simpleName
        'trt'      | TermRelationshipType.class.simpleName
        'tr'       | TermRelationship.class.simpleName
        'cs'       | CodeSet.class.simpleName
        'df'       | DataFlow.class.simpleName
        'dcc'      | DataClassComponent.class.simpleName
        'dec'      | DataElementComponent.class.simpleName
        'dm'       | DataModel.class.simpleName
        'dc'       | DataClass.class.simpleName
        'dt'       | DataType.class.simpleName
        'de'       | DataElement.class.simpleName
        'ev'       | EnumerationValue.class.simpleName
        'csc'      | ClassificationScheme.class.simpleName
        'cl'       | Classifier.class.simpleName
    }
}


