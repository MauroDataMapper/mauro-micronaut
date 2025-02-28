package uk.ac.ox.softeng.mauro.controller.folder

import com.fasterxml.jackson.core.Versioned
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.core.annotation.NonNull
import io.micronaut.core.annotation.Nullable
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.server.multipart.MultipartBody
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import uk.ac.ox.softeng.mauro.ErrorHandler
import uk.ac.ox.softeng.mauro.api.Paths
import uk.ac.ox.softeng.mauro.api.folder.VersionedFolderApi
import uk.ac.ox.softeng.mauro.api.model.PermissionsDTO
import uk.ac.ox.softeng.mauro.audit.Audit
import uk.ac.ox.softeng.mauro.controller.model.ModelController
import uk.ac.ox.softeng.mauro.domain.datamodel.DataModel
import uk.ac.ox.softeng.mauro.domain.folder.Folder
import uk.ac.ox.softeng.mauro.domain.folder.FolderService
import uk.ac.ox.softeng.mauro.domain.model.version.CreateNewVersionData
import uk.ac.ox.softeng.mauro.domain.model.version.FinaliseData
import uk.ac.ox.softeng.mauro.persistence.cache.ModelCacheableRepository.FolderCacheableRepository
import uk.ac.ox.softeng.mauro.persistence.folder.FolderContentRepository
import uk.ac.ox.softeng.mauro.web.ListResponse

@Slf4j
@CompileStatic
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
class VersionedFolderController extends ModelController<Folder> implements VersionedFolderApi {

    private static final String MY_CLASS_TYPE="VersionedFolder"

    @Inject
    FolderContentRepository folderContentRepository

    @Inject
    FolderService folderService

    VersionedFolderController(FolderCacheableRepository folderRepository, FolderContentRepository folderContentRepository, FolderService folderService) {
        super(Folder, folderRepository, folderRepository, folderContentRepository, folderService)
        this.folderService = folderService
    }

    @Get(Paths.VERSIONED_FOLDER_ID)
    Folder show(UUID id) {
        super.show(id)
    }

    @Get(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder show(UUID parentId, UUID id) {
        super.show(id)
    }

    @Audit
    @Post(Paths.VERSIONED_FOLDER_LIST)
    Folder create(@Body Folder folder) {
        cleanBody(folder)
        updateCreationProperties(folder)

        folder.setVersionable(true)

        pathRepository.readParentItems(folder)
        folder.updatePath()

        folderRepository.save(folder)
    }

    @Audit
    @Transactional
    @Post(Paths.CHILD_VERSIONED_FOLDER_LIST)
    Folder create(UUID parentId, @Body @NonNull Folder folder) {
        folder.setVersionable(true)
        super.create(parentId, folder)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_ID)
    Folder update(UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Audit
    @Put(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    Folder update(UUID parentId, UUID id, @Body @NonNull Folder folder) {
        super.update(id, folder)
    }

    @Get(Paths.VERSIONED_FOLDER_LIST)
    ListResponse<Folder> listAll() {

        final ListResponse<Folder> listResponse=super.listAll()

        listResponse.items =listResponse.items.findAll { MY_CLASS_TYPE == ((Folder) it).getClass_() }
        listResponse.count=listResponse.items.size()

        return listResponse
    }

    @Get(Paths.CHILD_VERSIONED_FOLDER_LIST)
    ListResponse<Folder> list(UUID parentId) {

        final ListResponse<Folder> listResponse=super.list(parentId)

        listResponse.items =listResponse.items.findAll { MY_CLASS_TYPE == ((Folder) it).getClass_() }
        listResponse.count=listResponse.items.size()

        return listResponse
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_FINALISE)
    Folder finalise(UUID id, @Body FinaliseData finaliseData) {
        super.finalise(id, finaliseData)
    }

    @Audit
    @Transactional
    @Put(Paths.VERSIONED_FOLDER_NEW_BRANCH_MODEL_VERSION)
    Folder createNewBranchModelVersion(UUID id, @Body @Nullable CreateNewVersionData createNewVersionData) {
        super.createNewBranchModelVersion(id, createNewVersionData)
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder,permanent)
    }

    @Audit
    @Transactional
    @Delete(Paths.FOLDER_CHILD_VERSIONED_FOLDER_ID)
    HttpResponse delete(UUID parentId, UUID id, @Body @Nullable Folder folder, @Nullable @QueryValue Boolean permanent) {
        permanent = permanent ?: true
        super.delete(id, folder,permanent)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    @Transactional
    Folder allowReadByAuthenticated(UUID id) {
        super.putReadByAuthenticated(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_AUTHENTICATED)
    HttpResponse revokeReadByAuthenticated(UUID id) {
        super.deleteReadByAuthenticated(id)
    }

    @Audit
    @Put(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    @Transactional
    Folder allowReadByEveryone(UUID id) {
        super.putReadByEveryone(id) as Folder
    }

    @Audit
    @Transactional
    @Delete(Paths.VERSIONED_FOLDER_READ_BY_EVERYONE)
    HttpResponse revokeReadByEveryone(UUID id) {
        super.deleteReadByEveryone(id)
    }

    @Get(Paths.VERSIONED_FOLDER_PERMISSIONS)
    @Override
    PermissionsDTO permissions(UUID id) {
        super.permissions(id)
    }
    @Get(Paths.VERSIONED_FOLDER_DOI)
    @Override
    Map doi(UUID id) {
        ErrorHandler.handleErrorOnNullObject(HttpStatus.SERVICE_UNAVAILABLE, "Doi", "Doi is not implemented")
        return null
    }
}