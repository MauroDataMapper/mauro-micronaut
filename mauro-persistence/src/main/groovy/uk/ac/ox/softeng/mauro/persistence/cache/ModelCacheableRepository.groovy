package uk.ac.ox.softeng.mauro.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.context.annotation.Bean
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.folder.FolderRepository
import uk.ac.ox.softeng.mauro.persistence.model.ModelRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.CodeSetRepository
import uk.ac.ox.softeng.mauro.persistence.terminology.TerminologyRepository

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class ModelCacheableRepository<M extends Model> extends AdministeredItemCacheableRepository<M> implements ModelRepository<M> {

    ModelRepository<M> repository

    ModelCacheableRepository(ModelRepository<M> itemRepository) {
        super(itemRepository)
        repository = itemRepository
    }

    @Override
    List<M> readAllByFolder(Folder folder) {
        super.readAllByParent(folder)
    }

    // not cached
    @Override
    List<M> readAll() {
        repository.readAll()
    }

    // Cacheable Model Repository definitions

    @Bean
    @CompileStatic
    static class TerminologyCacheableRepository extends ModelCacheableRepository<Terminology> {
        TerminologyCacheableRepository(TerminologyRepository terminologyRepository) {
            super(terminologyRepository)
        }
    }

    @Bean
    @CompileStatic
    static class FolderCacheableRepository extends ModelCacheableRepository<Folder> {
        FolderCacheableRepository(FolderRepository folderRepository) {
            super(folderRepository)
        }
    }

    @Bean
    @CompileStatic
    static class CodeSetCacheableRepository extends ModelCacheableRepository<CodeSet> {
       CodeSetCacheableRepository(CodeSetRepository codeSetRepository) {
            super(codeSetRepository)
        }
    }

}