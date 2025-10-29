package org.maurodata.controller.classifier

import jakarta.inject.Inject
import org.maurodata.ErrorHandler
import org.maurodata.api.Paths
import org.maurodata.api.classifier.ClassificationSchemeApi
import org.maurodata.api.model.PermissionsDTO
import org.maurodata.audit.Audit
import org.maurodata.controller.model.ModelController
import org.maurodata.domain.classifier.ClassificationScheme
import org.maurodata.domain.diff.ObjectDiff
import org.maurodata.domain.facet.EditType
import org.maurodata.domain.model.version.CreateNewVersionData
import org.maurodata.domain.security.Role
import org.maurodata.persistence.cache.ModelCacheableRepository
import org.maurodata.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import org.maurodata.persistence.classifier.ClassificationSchemeContentRepository
import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Delete
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Put
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.web.PaginationParams

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class ClassificationSchemeController extends ModelController<ClassificationScheme> implements ClassificationSchemeApi {



    ClassificationSchemeContentRepository classificationSchemeContentRepository


    ClassificationSchemeController(ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository,
                                   FolderCacheableRepository folderRepository, ClassificationSchemeContentRepository classificationSchemeContentRepository) {
        super(ClassificationScheme, classificationSchemeCacheableRepository, folderRepository, classificationSchemeContentRepository)
        this.classificationSchemeContentRepository = classificationSchemeContentRepository
    }

    @Audit
    @Get(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Post(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ClassificationScheme create(UUID folderId, @Body @NonNull ClassificationScheme classificationScheme) {
        super.create(folderId, classificationScheme)
    }

    @Audit
    @Put(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme update(UUID id, @Body @NonNull ClassificationScheme classificationScheme) {
        super.update(id, classificationScheme)
    }

    @Audit
    @Transactional
    @Delete(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable ClassificationScheme classificationScheme, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, classificationScheme, permanent)
    }

    @Audit
    @Get(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE_PAGED)
    ListResponse<ClassificationScheme> list(UUID folderId, @Nullable PaginationParams params = new PaginationParams()) {
        super.list(folderId, params)
    }

    @Audit
    @Get(Paths.CLASSIFICATION_SCHEMES_LIST_PAGED)
    ListResponse<ClassificationScheme> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        super.listAll(params)
    }

    @Audit(title = EditType.COPY, description = "Create new version of classification scheme")
    @Transactional
    @Put(Paths.CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION)
    ClassificationScheme createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit
    @Get(Paths.CLASSIFICATION_SCHEMES_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModel(id, namespace, name, version)
    }

    @Audit(title = EditType.IMPORT, description = "Import classification scheme")
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post(Paths.CLASSIFICATION_SCHEMES_IMPORT)
    ListResponse<ClassificationScheme> importModel(@Body MultipartBody body, String namespace, String name, @Nullable String version) {
        super.importModel(body, namespace, name, version)
    }


    @Audit
    @Get(Paths.CLASSIFICATION_SCHEMES_DIFF)
    ObjectDiff diffModels(@NonNull UUID id, @NonNull UUID otherId) {
        ClassificationScheme classificationScheme = modelRepository.loadWithContent(id)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classificationScheme, "Item not found: $id")
        ClassificationScheme otherClassificationScheme = modelRepository.loadWithContent(otherId)
        ErrorHandler.handleErrorOnNullObject(HttpStatus.NOT_FOUND, classificationScheme, "Item not found: $otherId")


        accessControlService.checkRole(Role.READER, classificationScheme)
        accessControlService.checkRole(Role.READER, otherClassificationScheme)

        classificationScheme.setAssociations()
        otherClassificationScheme.setAssociations()

        pathRepository.readParentItems(classificationScheme)
        classificationScheme.updatePath()

        pathRepository.readParentItems(otherClassificationScheme)
        otherClassificationScheme.updatePath()

        classificationScheme.diff(otherClassificationScheme)
    }

    @Audit
    @Put(Paths.CLASSIFICATION_SCHEMES_READ_BY_AUTHENTICATED)
    @Transactional
    ClassificationScheme allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as ClassificationScheme
    }

    @Audit
    @Transactional
    @Delete(Paths.CLASSIFICATION_SCHEMES_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.CLASSIFICATION_SCHEMES_READ_BY_EVERYONE)
    @Transactional
    ClassificationScheme allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as ClassificationScheme
    }

    @Audit
    @Transactional
    @Delete(Paths.CLASSIFICATION_SCHEMES_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Get(Paths.CLASSIFICATION_SCHEMES_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }
}
