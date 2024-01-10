package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.AdministeredItemRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.context.annotation.Bean
import reactor.core.publisher.Flux

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class CacheableModelRepository<M extends Model> extends CacheableAdministeredItemRepository<M> implements ModelRepository<M> {

    ModelRepository<M> repository

    CacheableModelRepository(ModelRepository<M> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    @Override
    Flux<M> readAllByFolder(Folder folder) {
        super.readAllByParent(folder)
    }

    // not cached
    @Override
    Flux<M> readAll() {
        repository.readAll()
    }

    // Cacheable Model Repository definitions

    @Bean
    @CompileStatic
    static class CacheableTerminologyRepository extends CacheableModelRepository<Terminology> {
        CacheableTerminologyRepository(TerminologyRepository terminologyRepository) {
            super(terminologyRepository)
        }
    }

    @Bean
    @CompileStatic
    static class CacheableFolderRepository extends CacheableModelRepository<Folder> {
        CacheableFolderRepository(FolderRepository folderRepository) {
            super(folderRepository)
        }
    }
}