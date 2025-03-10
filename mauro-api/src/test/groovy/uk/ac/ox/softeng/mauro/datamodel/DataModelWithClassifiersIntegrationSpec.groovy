package uk.ac.ox.softeng.mauro.datamodel

import uk.ac.ox.softeng.mauro.api.classifier.ClassificationSchemeApi
import uk.ac.ox.softeng.mauro.api.classifier.ClassifierApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataClassApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataElementApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataModelApi
import uk.ac.ox.softeng.mauro.api.datamodel.DataTypeApi
import uk.ac.ox.softeng.mauro.api.datamodel.EnumerationValueApi
import uk.ac.ox.softeng.mauro.api.facet.MetadataApi
import uk.ac.ox.softeng.mauro.api.facet.ReferenceFileApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataApi
import uk.ac.ox.softeng.mauro.api.facet.SummaryMetadataReportApi
import uk.ac.ox.softeng.mauro.api.folder.FolderApi

import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.runtime.EmbeddedApplication
import jakarta.inject.Inject
import jakarta.inject.Singleton
import spock.lang.Shared
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.classifier.Classifier
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.facet.Metadata
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.testing.CommonDataSpec

@ContainerizedTest
@Singleton
class DataModelWithClassifiersIntegrationSpec extends CommonDataSpec {

    @Shared
    UUID folderId
    @Shared
    UUID classificationSchemeId
    @Shared
    UUID classifierId1
    @Shared
    UUID classifierId2

    void setup(){
        folderId = folderApi.create(folder()).id
        classificationSchemeId = classificationSchemeApi.create(folderId, classificationSchemePayload()).id
        classifierId1 = classifierApi.create(classificationSchemeId, classifierPayload()).id
        classifierId2 = classifierApi.create(classificationSchemeId, classifierPayload()).id
    }


    void 'create datamodel with classifier -should create'() {
        when:
        DataModel created  = dataModelApi.create(folderId,
                new DataModel(label: 'test label',
                 description: 'test description',
                 author: 'an author',
                 classifiers: [ new Classifier(id: classifierId1)]))
        then:
        created
        created.classifiers.size() == 1
        created.classifiers[0].id == classifierId1

        when:
        Classifier retrieved1 = classifierApi.getAdministeredItemClassifier("dataModel",created.id,classifierId1)
        then:
        retrieved1
        retrieved1.id == classifierId1

        when:
        retrieved1 = classifierApi.getAdministeredItemClassifier("dataModel",created.id,classifierId2)
        then:
        !retrieved1
    }

    void 'create datamodel with non existing classifier -should throw exception indicating classifier not found'() {
        when:
        DataModel dataModel = dataModelApi.create(folderId,
                new DataModel(label: 'test label',
                                description: 'test description',
                                author: 'an author',
                                classifiers: [ new Classifier(id:  UUID.randomUUID())]))
        then:
        !dataModel
    }

    void 'update datamodel with non existing classifier -should throw exception indicating classifier not found'() {
        when:
        DataModel created = dataModelApi.create(folderId, dataModelPayload())
        then:
        created

        when:
        DataModel updated = dataModelApi.update(created.id,
                new DataModel(
                    classifiers: [ new Classifier(id:  UUID.randomUUID())]))
        then:
        !updated
    }



    void 'update datamodel with classifier -should update'() {
        when:
        DataModel created  = dataModelApi.create(folderId, dataModelPayload())
        then:
        created

        when:
        DataModel updated  = dataModelApi.update(created.id,
              new DataModel( classifiers: [ new Classifier(id:  classifierId2)]))
        then:
        updated
        updated.classifiers.size() == 1
        updated.classifiers[0].id == classifierId2

        when:
        Classifier retrieved = classifierApi.getAdministeredItemClassifier(
            "dataModel", created.id, classifierId2)
        then:

        retrieved
        retrieved.id == classifierId2

        when:
        retrieved = classifierApi.getAdministeredItemClassifier(
            "dataModel", created.id, classifierId1)
        then:
        !retrieved
    }


    void 'delete datamodel with classifier and facets - should delete all items'() {
        given:

        DataModel dataModel = dataModelApi.create( folderId,
                           new DataModel( label: 'test label',
                            description: 'test description',
                            author: 'an author',
                            classifiers: [new Classifier(id: classifierId1)]))

        Metadata metadata = metadataApi.create("dataModels", dataModel.id, metadataPayload())

        when:
        HttpResponse response = dataModelApi.delete(dataModel.id, new DataModel())
        then:
        response.status == HttpStatus.NO_CONTENT

        when:
        metadata = metadataApi.show("dataModel", dataModel.id, metadata.id)

        then:
        !metadata

        when:
        Classifier classifier = classifierApi.getAdministeredItemClassifier("dataModel", dataModel.id, classifierId1)

        then:
        !classifier
    }
}
