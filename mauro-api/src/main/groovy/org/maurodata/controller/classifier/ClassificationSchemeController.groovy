package org.maurodata.controller.classifier

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
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

import org.maurodata.web.ListResponse

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpRequest
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
import io.micronaut.http.annotation.Part
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import org.maurodata.web.PaginationParams
import org.reactivestreams.Publisher

@Slf4j
@Controller
@CompileStatic
@Secured(SecurityRule.IS_ANONYMOUS)
class ClassificationSchemeController extends ModelController<ClassificationScheme> implements ClassificationSchemeApi {

    ClassificationSchemeController(ModelCacheableRepository.ClassificationSchemeCacheableRepository classificationSchemeCacheableRepository,
                                   FolderCacheableRepository folderRepository) {
        super(ClassificationScheme, classificationSchemeCacheableRepository, folderRepository)
    }

    @Audit
    @Operation(operationId = 'showClassificationScheme', summary = "Get a classification scheme", description = "Returns a classification scheme.")
    @Get(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme show(UUID id) {
        super.show(id)
    }

    @Audit
    @Transactional
    @Operation(operationId = 'createClassificationScheme', summary = "Create a classification scheme", description = "Creates a classification scheme.")
    @Post(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE)
    ClassificationScheme create(UUID folderId, @Body @NonNull ClassificationScheme classificationScheme) {
        super.create(folderId, classificationScheme)
    }

    @Audit
    @Operation(operationId = 'updateClassificationScheme', summary = "Update a classification scheme", description = "Updates a classification scheme.")
    @Put(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    ClassificationScheme update(UUID id, @Body @NonNull ClassificationScheme classificationScheme) {
        super.update(id, classificationScheme)
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classification scheme", description = "Deletes a classification scheme.")
    @Delete(Paths.CLASSIFICATION_SCHEMES_ID_ROUTE)
    HttpResponse delete(UUID id, @Body @Nullable ClassificationScheme classificationScheme, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, classificationScheme, permanent)
    }

    @Audit
    @Operation(operationId = 'listClassificationScheme', summary = "List the classification schemes", description = "Returns the classification schemes.")
    @Get(Paths.FOLDER_CLASSIFICATION_SCHEMES_ROUTE_PAGED)
    ListResponse<ClassificationScheme> list(UUID folderId, @Nullable PaginationParams params = new PaginationParams()) {
        super.list(folderId, params)
    }

    @Audit
    @Operation(operationId = 'listAllClassificationSchemePaged', summary = "List the classification schemes", description = "Returns the classification schemes.")
    @Get(Paths.CLASSIFICATION_SCHEMES_LIST_PAGED)
    ListResponse<ClassificationScheme> listAll(@Nullable PaginationParams params = new PaginationParams()) {
        super.listAll(params)
    }

    @Audit(title = EditType.COPY, description = "Create new version of classification scheme")
    @Transactional
    @Operation(summary = "Update a classification scheme", description = "Updates a classification scheme.")
    @Put(Paths.CLASSIFICATION_SCHEMES_BRANCH_MODEL_VERSION)
    ClassificationScheme createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit(title = EditType.EXPORT, description = 'Export classification scheme')
    @Operation(operationId = 'exportModelClassificationScheme', summary = "Get a classification scheme", description = "Returns a classification scheme.")
    @Get(Paths.CLASSIFICATION_SCHEMES_EXPORT)
    HttpResponse<byte[]> exportModel(UUID id, @Nullable String namespace, @Nullable String name, @Nullable String version) {
        super.exportModels(namespace, name, version, [id])
    }

    @Audit(title = EditType.EXPORT, description = 'Export classification schemes')
    @Operation(summary = "Export the classification scheme", description = "Exports the classification scheme.")
    @Post(Paths.CLASSIFICATION_SCHEMES_EXPORT_MANY)
    HttpResponse<byte[]> exportModels(@Nullable String namespace, @Nullable String name, @Nullable String version, @Body List<UUID> ids) {
        super.exportModels(namespace, name, version, ids)
    }

    @Audit(title = EditType.IMPORT, description = "Import classification scheme")
    @Transactional
    @ExecuteOn(TaskExecutors.IO)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(operationId = 'importModelClassificationScheme', summary = "Import the classification scheme", description = "Imports the classification scheme.")
    @Post(Paths.CLASSIFICATION_SCHEMES_IMPORT)
    ListResponse<ClassificationScheme> importModel(HttpRequest<?> request, @Part('importFile') @Nullable Publisher<StreamingFileUpload> importFile, String namespace, String name, @Nullable String version) {
        super.importModel(request, importFile, namespace, name, version)
    }


    @Audit
    @Operation(summary = "Get a classification scheme", description = "Returns a classification scheme. You must have read privileges on the item in question.")
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
    @Operation(summary = "Update a classification scheme", description = "Updates a classification scheme.")
    @Put(Paths.CLASSIFICATION_SCHEMES_READ_BY_AUTHENTICATED)
    @Transactional
    ClassificationScheme allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as ClassificationScheme
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classification scheme", description = "Deletes a classification scheme.")
    @Delete(Paths.CLASSIFICATION_SCHEMES_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Operation(summary = "Update a classification scheme", description = "Updates a classification scheme.")
    @Put(Paths.CLASSIFICATION_SCHEMES_READ_BY_EVERYONE)
    @Transactional
    ClassificationScheme allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as ClassificationScheme
    }

    @Audit
    @Transactional
    @ApiResponse(responseCode = "204", description = "No content - deleted successfully")
    @Operation(summary = "Delete a classification scheme", description = "Deletes a classification scheme.")
    @Delete(Paths.CLASSIFICATION_SCHEMES_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Operation(summary = "List the classification schemes", description = "Returns the classification schemes.")
    @Get(Paths.CLASSIFICATION_SCHEMES_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }
}
