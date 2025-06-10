package uk.ac.ox.softeng.mauro.persistence.cache

import uk.ac.ox.softeng.mauro.domain.model.version.ModelVersion

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.model.Model
import uk.ac.ox.softeng.mauro.domain.terminology.CodeSet
import uk.ac.ox.softeng.mauro.domain.terminology.Terminology
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassificationSchemeRepository
import uk.ac.ox.softeng.mauro.persistence.datamodel.DataModelRepository
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

    @Override
    @Nullable
    List<M> findAllByFolderId(UUID folderId) {
        return null
    }

    @Override
    List<M> readAllByFinalisedTrue() {
        repository.readAllByFinalisedTrue()
    }

    @Override
    M readByLabelAndModelVersion(String label, ModelVersion modelVersion) {
        repository.readByLabelAndModelVersion(label, modelVersion)
    }

    // Cacheable Model Repository definitions

    @CompileStatic
    @Singleton
    static class TerminologyCacheableRepository extends ModelCacheableRepository<Terminology> {
        TerminologyCacheableRepository(TerminologyRepository terminologyRepository) {
            super(terminologyRepository)
        }

        @Override
        Boolean handles(String domainType) {
            repository.handles(domainType)
        }
    }

    @CompileStatic
    @Singleton
    static class FolderCacheableRepository extends ModelCacheableRepository<Folder> {
        FolderCacheableRepository(FolderRepository folderRepository) {
            super(folderRepository)
        }

        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['folder', 'folders', 'versionedfolder', 'versionedfolders']
        }

        // not cached

        List<Folder> readAllRootFolders() {
            ((FolderRepository) repository).readAllRootFolders()
        }
    }

    @Singleton
    @CompileStatic
    static class CodeSetCacheableRepository extends ModelCacheableRepository<CodeSet> {
        CodeSetCacheableRepository(CodeSetRepository codeSetRepository) {
            super(codeSetRepository)
        }
    }

    @CompileStatic
    @Singleton
    static class DataModelCacheableRepository extends ModelCacheableRepository<DataModel> {
        DataModelCacheableRepository(DataModelRepository dataModelRepository) {
            super(dataModelRepository)
        }
    }

    @CompileStatic
    @Singleton
    static class ClassificationSchemeCacheableRepository extends ModelCacheableRepository<ClassificationScheme> {
        ClassificationSchemeCacheableRepository(ClassificationSchemeRepository classificationSchemeRepository) {
            super(classificationSchemeRepository as ModelRepository<ClassificationScheme>)
        }
    }

}