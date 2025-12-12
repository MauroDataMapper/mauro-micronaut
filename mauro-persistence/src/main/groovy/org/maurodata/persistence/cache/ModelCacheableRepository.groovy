package org.maurodata.persistence.cache

import io.micronaut.core.annotation.NonNull
import org.maurodata.domain.model.version.ModelVersion

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.cache.annotation.CacheConfig
import io.micronaut.core.annotation.Nullable
import jakarta.inject.Singleton
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.datamodel.DataModel
import org.maurodata.domain.folder.Folder
import org.maurodata.domain.model.Model
import org.maurodata.domain.terminology.CodeSet
import org.maurodata.domain.terminology.Term
import org.maurodata.domain.terminology.Terminology
import org.maurodata.persistence.ContentsService
import org.maurodata.persistence.classifier.ClassificationSchemeRepository
import org.maurodata.persistence.datamodel.DataModelRepository
import org.maurodata.persistence.folder.FolderRepository
import org.maurodata.persistence.model.ModelRepository
import org.maurodata.persistence.terminology.CodeSetRepository
import org.maurodata.persistence.terminology.TerminologyRepository
import org.maurodata.persistence.terminology.dto.CodeSetTermDTO

@Slf4j
@CompileStatic
@CacheConfig(cacheNames = 'items-cache', keyGenerator = StringCacheKeyGenerator)
class ModelCacheableRepository<M extends Model> extends AdministeredItemCacheableRepository<M> implements ModelRepository<M> {

    ModelRepository<M> repository

    ModelCacheableRepository(ModelRepository<M> itemRepository, ContentsService contentsService) {
        super(itemRepository, contentsService)
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
        repository.findAllByFolderId(folderId)
    }

    @Override
    List<M> readAllByFolderIdIn(Collection<UUID> folderIds) {
        repository.readAllByFolderIdIn(folderIds)
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
        TerminologyCacheableRepository(TerminologyRepository terminologyRepository, ContentsService contentsService) {
            super(terminologyRepository, contentsService)
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
        FolderCacheableRepository(FolderRepository folderRepository, ContentsService contentsService) {
            super(folderRepository, contentsService)
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
        CodeSetCacheableRepository(CodeSetRepository codeSetRepository, ContentsService contentsService) {
            super(codeSetRepository, contentsService)
        }
        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['codeset', 'codesets']
        }

        // Not cached
        Set<Term> readTerms(@NonNull UUID uuid) {
            ((CodeSetRepository) repository).readTerms(uuid)
        }

        CodeSet addTerm(@NonNull UUID uuid, @NonNull UUID termId) {
            ((CodeSetRepository) repository).addTerm(uuid, termId)
        }

        // Not cached
        Long removeAllAssociations(@NonNull Collection<UUID> uuids) {
            ((CodeSetRepository) repository).removeTermAssociations(uuids)
        }

        List<CodeSetTermDTO> getCodeSetTerms(@NonNull List<UUID> codeSetIds) {
            ((CodeSetRepository) repository).getCodeSetTerms(codeSetIds)
        }

    }

    @CompileStatic
    @Singleton
    static class DataModelCacheableRepository extends ModelCacheableRepository<DataModel> {
        DataModelCacheableRepository(DataModelRepository dataModelRepository, ContentsService contentsService) {
            super(dataModelRepository, contentsService)
        }

        @Override
        Boolean handles(String domainType) {
            return domainType != null && domainType.toLowerCase() in ['datamodel', 'datamodels']
        }

        List<DataModel> getAllModelsByNamespace(String namespace) {
            ((DataModelRepository) repository).getAllModelsByNamespace(namespace)
        }

    }

    @CompileStatic
    @Singleton
    static class ClassificationSchemeCacheableRepository extends ModelCacheableRepository<ClassificationScheme> {
        ClassificationSchemeCacheableRepository(ClassificationSchemeRepository classificationSchemeRepository, ContentsService contentsService) {
            super(classificationSchemeRepository, contentsService)
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