package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.util.logging.Slf4j
import jakarta.inject.Inject
import spock.lang.Specification
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.persistence.ContainerizedTest
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.DataModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository

@ContainerizedTest
@Slf4j
class CacheableRepositorySpec extends Specification {

    @Inject
    FolderCacheableRepository folderCacheableRepository

    @Inject
    DataModelCacheableRepository dataModelCacheableRepository

    void 'check folder cache is immutable'() {
        given:
        Folder folder = new Folder(label: 'new folder', description: 'initial description')
        Folder saved = folderCacheableRepository.save(folder)
        UUID folderId = saved.id

        when: 'folder is retrieved using readById'
        Folder retrieved = folderCacheableRepository.readById(folderId)
        retrieved.label = 'updated folder'
        retrieved.description = 'updated description'
        Folder retrievedAgain = folderCacheableRepository.readById(folderId)

        then: 'update is not reflected in cache'
        retrieved !== saved

        retrievedAgain !== retrieved
        retrievedAgain.label == 'new folder'
        retrievedAgain.description == 'initial description'

        when: 'folder is retrieved using findById'
        retrieved = folderCacheableRepository.readById(folderId)
        retrieved.label = 'updated folder'
        retrieved.description = 'updated description'

        retrievedAgain = folderCacheableRepository.findById(folderId)

        then: 'update is not reflected in cache'
        retrieved !== saved

        retrievedAgain !== retrieved
        retrievedAgain.label == 'new folder'
        retrievedAgain.description == 'initial description'

        when: 'update is saved'
        folderCacheableRepository.update(retrieved)
        retrievedAgain = folderCacheableRepository.readById(folderId)

        then: 'cache is updated'
        retrievedAgain.label == 'updated folder'
        retrievedAgain.description == 'updated description'
    }

    void 'check cache of list of datamodels is immutable'() {
        given:
        Folder folder = new Folder(label: 'parent folder')
        folder = folderCacheableRepository.save(folder)
        UUID folderId = folder.id
        (1..10).each {Integer i ->
            DataModel dataModel = new DataModel(label: "dm $i", folder: folder)
            dataModelCacheableRepository.save(dataModel)
        }

        when: 'all datamodels in folder are retrieved and updated'
        List<DataModel> dataModelsRetrieved = dataModelCacheableRepository.readAllByParent(folder)
        dataModelsRetrieved.each {
            it.label = 'updated ' + it.label
        }
        List<DataModel> dataModelsRetrievedAgain = dataModelCacheableRepository.readAllByParent(folder)

        then: 'updates are not reflected in cache'
        dataModelsRetrievedAgain.label.sort() == (1..10).collect{i -> "dm $i".toString()}.sort()

        when: 'updates are saved'
        dataModelsRetrieved.each {
            dataModelCacheableRepository.update(it)
        }
        dataModelsRetrievedAgain = dataModelCacheableRepository.readAllByParent(folder)

        then: 'cache is updated'
        dataModelsRetrievedAgain.label.sort() == (1..10).collect{i -> "updated dm $i".toString()}.sort()
    }

    void 'check performance of cache retrieval'() {
        given:
        Folder folder = new Folder(label: 'performance test folder')
        folder = folderCacheableRepository.save(folder)
        UUID folderId = folder.id

        when: 'get folder from cache 1 million times'
        long startTime = System.currentTimeMillis()
        Folder previous
        Folder retrieved
        1e6.times {
            retrieved = folderCacheableRepository.findById(folderId)
            assert previous !== retrieved
            previous = retrieved
        }
        long endTime = System.currentTimeMillis()

        // on Apple M2 Max Mac Studio this takes: 1.2 seconds
        // warning: if debug logging in io.micronaut.cache is enabled, this takes longer!
        then: 'takes less than 10 seconds'
        endTime - startTime < 10 * 1000
        log.info "folderCacheableRepository.findById took ${endTime - startTime}ms for 1000000 invocations"
    }
}
