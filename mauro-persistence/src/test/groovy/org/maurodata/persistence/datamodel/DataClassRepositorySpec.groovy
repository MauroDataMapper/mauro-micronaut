package org.maurodata.persistence.datamodel

import org.maurodata.domain.datamodel.DataClass
import org.maurodata.domain.datamodel.DataElement
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.datamodel.DataType
import org.maurodata.domain.folder.Folder
import org.maurodata.persistence.ContainerizedTest
import org.maurodata.persistence.cache.AdministeredItemCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository

import jakarta.inject.Inject
import spock.lang.Shared
import spock.lang.Specification

import static org.maurodata.persistence.cache.ItemCacheableRepository.*


@ContainerizedTest
class DataClassRepositorySpec extends Specification {

    @Inject
    @Shared
    DataModelContentRepository dataModelContentRepository

    @Inject
    @Shared
    DataClassContentRepository dataClassContentRepository

    @Inject
    @Shared
    ModelCacheableRepository.DataModelCacheableRepository dataModelCacheableRepository

    @Inject
    DataModelRepository dataModelRepository

    @Inject
    AdministeredItemCacheableRepository.DataClassCacheableRepository dataClassCacheableRepository

    @Inject
    DataClassRepository dataClassRepository

    @Inject
    AdministeredItemCacheableRepository.DataElementCacheableRepository dataElementRepository

    @Inject
    AdministeredItemCacheableRepository.DataTypeCacheableRepository dataTypeRepository

    @Inject
    AdministeredItemCacheableRepository.EnumerationValueCacheableRepository enumerationValueRepository

    @Inject
    @Shared
    ModelCacheableRepository.FolderCacheableRepository folderRepository

    @Shared
    Folder myFirstFolder

    @Shared
    DataModel sharedDataModel

    void setupSpec() {
        myFirstFolder = folderRepository.save(new Folder(
            label: "My first Folder"
        ))
        DataModel dataModel = DataModel.build {
            label "My first data model"
            description "Description here"
            folder myFirstFolder
        }
        sharedDataModel = dataModelCacheableRepository.save(dataModel)
    }

    void "test saving and retrieving a class"() {
        when:
            List<DataClass> response = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(sharedDataModel)
        then:
            response.size() == 0

        when:
            response = dataClassCacheableRepository.findAllByParent(sharedDataModel)
        then:
            response.size() == 0

        when:
            UUID newDataClassId = dataClassCacheableRepository.save(new DataClass(label: 'My test DataClass', dataModel: sharedDataModel)).id
            response = dataClassCacheableRepository.readAllByDataModelAndParentDataClassIsNull(sharedDataModel)
        then:
            response.size() == 1
            response.first().label == 'My test DataClass'

        when:
            response = dataClassCacheableRepository.findAllByParent(sharedDataModel)
        then:
            response.size() == 1
            response.first().label == 'My test DataClass'

        when:
            DataClass retrieved = dataClassCacheableRepository.readById(newDataClassId)

        then:
            retrieved.label == 'My test DataClass'

        when:
            retrieved = dataClassCacheableRepository.findById(newDataClassId)

        then:
            retrieved.label == 'My test DataClass'

    }

    void "test updating a class"() {
        when:
            DataClass dataClass = dataClassCacheableRepository.save(new DataClass(label: 'My test DataClass', dataModel: sharedDataModel))
            UUID newDataClassId = dataClass.id
            DataClass retrieved = dataClassCacheableRepository.readById(newDataClassId)

        then:
            retrieved.label == 'My test DataClass'

        when:
            // Whenever we update a thing, it needs the version number of the original
            dataClassCacheableRepository.update(
                new DataClass(
                    label: 'My test DataClass (updated)',
                    dataModel: sharedDataModel,
                    id: newDataClassId,
                    version: dataClass.version))
            retrieved = dataClassCacheableRepository.readById(newDataClassId)
        then:
            retrieved.label == 'My test DataClass (updated)'

        when:
            retrieved = dataClassCacheableRepository.findById(newDataClassId)

        then:
            retrieved.label == 'My test DataClass (updated)'

        when:
            List<DataClass> response = dataClassCacheableRepository.findAllByParent(sharedDataModel)

        then:
            response.size() == 1
            response.first().label == 'My test DataClass (updated)'

    }

    void "test extending a class"() {
        when:
        DataClass dataClass1 = dataClassCacheableRepository.save(new DataClass(label: 'My test DataClass 1', dataModel: sharedDataModel))
        DataClass dataClass2 = dataClassCacheableRepository.save(new DataClass(label: 'My test DataClass 2', dataModel: sharedDataModel))
        DataClass retrieved = dataClassCacheableRepository.readById(dataClass2.id)

        then:
        retrieved.label == 'My test DataClass 2'

        when:
            // Whenever we update a thing, it needs the version number of the original
            dataClassRepository.update(
                new DataClass(
                    label: 'My test DataClass 2 (updated)',
                    dataModel: sharedDataModel,
                    id: dataClass2.id,
                    version: dataClass2.version))
            dataClassRepository.addDataClassExtensionRelationship(dataClass2.id, dataClass1.id)
            retrieved = dataClassRepository.findById(dataClass2.id)

        then:
            retrieved.label == 'My test DataClass 2 (updated)'
            retrieved.extendsDataClasses.size() == 1
            retrieved.extendsDataClasses.first().label == 'My test DataClass 1'
            retrieved.extendsDataClasses.first().id == dataClass1.id

            // By default we get the list of classes that we extend, but not the list of classes that extend us.
            retrieved.extendedBy.size() == 0

    }

    void "test extending a class with saving content"() {
        when:
        DataClass dataClass1 = dataClassContentRepository.saveWithContent(new DataClass(label: 'My test DataClass 1', dataModel: sharedDataModel))

        DataClass dataClass2 = dataClassContentRepository.saveWithContent(
            new DataClass(
                label: 'My test DataClass 2',
                dataModel: sharedDataModel,
                extendsDataClasses: [new DataClass(id: dataClass1.id)]))

        DataClass retrieved1 = dataClassRepository.findById(dataClass1.id)
        DataClass retrieved2 = dataClassRepository.findById(dataClass2.id)

        then:
        retrieved1.label == 'My test DataClass 1'
        retrieved2.label == 'My test DataClass 2'

        retrieved1.extendsDataClasses.size() == 0
        retrieved2.extendsDataClasses.size() == 1
        retrieved2.extendsDataClasses.first().label == 'My test DataClass 1'
        retrieved2.extendsDataClasses.first().id == dataClass1.id
    }

    void "test extending a class with saving content identified by label"() {
        when:
        DataClass dataClass1 = dataClassContentRepository.saveWithContent(new DataClass(label: 'My test DataClass 1', dataModel: sharedDataModel))

        DataClass dataClass2 = dataClassContentRepository.saveWithContent(
            new DataClass(
                label: 'My test DataClass 2',
                dataModel: sharedDataModel,
                extendsDataClasses: [new DataClass(label: dataClass1.label)]))

        DataClass retrieved1 = dataClassRepository.findById(dataClass1.id)
        DataClass retrieved2 = dataClassRepository.findById(dataClass2.id)

        then:
        retrieved1.label == 'My test DataClass 1'
        retrieved2.label == 'My test DataClass 2'

        retrieved1.extendsDataClasses.size() == 0
        retrieved2.extendsDataClasses.size() == 1
        retrieved2.extendsDataClasses.first().label == 'My test DataClass 1'
        retrieved2.extendsDataClasses.first().id == dataClass1.id

        // Won't have this value so have to query for it initially...
        when:
        DataClass dc = dataClassCacheableRepository.cachedLookupById(FIND_BY_ID, 'DataClass', retrieved2.id)

        then:
        dc.extendsDataClasses.size() == 1
        dc.extendsDataClasses.first().label == 'My test DataClass 1'
        dc.extendsDataClasses.first().id == dataClass1.id

        // Now retrieve it from the cache
        when:
        dc = dataClassCacheableRepository.cachedLookupById(FIND_BY_ID, 'DataClass', retrieved2.id)

        then:
        dc.extendsDataClasses.size() == 1
        dc.extendsDataClasses.first().label == 'My test DataClass 1'
        dc.extendsDataClasses.first().id == dataClass1.id

    }
}
