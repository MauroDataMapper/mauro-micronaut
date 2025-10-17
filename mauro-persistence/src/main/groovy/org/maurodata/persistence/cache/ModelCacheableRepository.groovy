package org.maurodata.persistence.cache

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.model.version.ModelVersion
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.classifier.ClassificationSchemeRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.folder.FolderRepository
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.persistence.terminology.CodeSetRepository
import org.maurodata.persistence.terminology.TerminologyRepository

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

        @Override
        Boolean handles(Class clazz) {
            repository.handles(clazz)
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
        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['codeset', 'codesets']
        }

    }

    @CompileStatic
    @Singleton
    static class DataModelCacheableRepository extends ModelCacheableRepository<DataModel> {
        DataModelCacheableRepository(DataModelRepository dataModelRepository) {
            super(dataModelRepository)
        }

        List<DataModel> findAllByLabelAndBranchName(String label, String branchName) {
            ((DataModelRepository) repository).findAllByLabelAndBranchName(label, branchName)
        }
        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['datamodel', 'datamodels']
        }
    }

    @CompileStatic
    @Singleton
    static class ClassificationSchemeCacheableRepository extends ModelCacheableRepository<ClassificationScheme> {
        ClassificationSchemeCacheableRepository(ClassificationSchemeRepository classificationSchemeRepository) {
            super(classificationSchemeRepository as ModelRepository<ClassificationScheme>)
        }

        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['classificationscheme', 'classificationschemes']
        }

        @Override
        Boolean handles(Class clazz) {
            return repository.handles(clazz)
        }
    }

}