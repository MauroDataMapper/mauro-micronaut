package uk.ac.ox.softeng.mauro.controller.classifier

import uk.ac.ox.softeng.mauro.ErrorHandler

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.http.server.types.files.StreamedFile
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.controller.Paths
import uk.ac.ox.softeng.mauro.domain.classifier.ClassificationScheme
import uk.ac.ox.softeng.mauro.domain.diff.ObjectDiff
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.security.Role
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.classifier.ClassificationSchemeContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class ClassificationSchemeController extends ModelController<ClassificationScheme> {

    ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository

    ClassificationSchemeContentRepository classificationSchemeContentRepository


    ClassificationSchemeController(ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository, FolderCacheableRepository folderRepository, ClassificationSchemeContentRepository classificationSchemeContentRepository) {
        super(ClassificationScheme, classificationSchemeCacheableRepository, folderRepository, classificationSchemeContentRepository)
        this.classificationSchemeContentRepository = classificationSchemeContentRepository
    }

    @Get(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme show(UUID id) {
        super.show(id)
    }

    @Transactional
    @Post('/folders/{folderId}/classificationSchemes')
    ClassificationScheme create(UUID folderId, @Body @NonNull ClassificationScheme classificationScheme) {
        super.create(folderId, classificationScheme)
    }

    @Put(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme update(UUID id, @Body @NonNull ClassificationScheme classificationScheme) {
        super.update(id, classificationScheme)
    }

    @Transactional
    @Delete(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    HttpStatus delete(UUID id, @Body @Nullable ClassificationScheme classificationScheme) {
        super.delete(id, classificationScheme)
    }


    @Get(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ListResponse<ClassificationScheme> list(UUID folderId) {
        super.list(folderId)
    }

    @Get(Paths.CLASSIFICATION_SCHEMES_ROUTE)
    ListResponse<ClassificationScheme> listAll() {
        super.listAll()
    }


    @Transactional
    @Put(Paths.CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION)
    ClassificationScheme createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Get('/classificationSchemes/{id}/export{/namespace}{/name}{/version}')
    StreamedFile exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post('/classificationSchemes/import/{namespace}/{name}{/version}')
    ListResponse<ClassificationScheme> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }

    @Get(Paths.CLASSIFICATION_SCHEMES_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        ClassificationScheme classificationScheme = modelContentRepository.findWithContentById(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classificationScheme, "Item not found: $id")
        ClassificationScheme otherClassificationScheme = modelContentRepository.findWithContentById(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classificationScheme, "Item not found: $otherId")


        accessControlService.checkRole(Role.READER, classificationScheme)
        accessControlService.checkRole(Role.READER, otherClassificationScheme)

        classificationScheme.setAssociations()
        otherClassificationScheme.setAssociations()
        classificationScheme.diff(otherClassificationScheme)
    }
}
